/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.knowledge.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.knowledge.source.ISyncSource;
import neatlogic.framework.knowledge.generation.KnowledgeGenerationSources;
import neatlogic.framework.knowledge.source.SyncSourceFactory;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import neatlogic.framework.fulltextindex.core.IFullTextIndexHandler;
import neatlogic.framework.fulltextindex.dto.fulltextindex.FullTextIndexVo;
import neatlogic.framework.knowledge.constvalue.KnowledgeDocumentOperate;
import neatlogic.framework.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import neatlogic.framework.knowledge.constvalue.KnowledgeFullTextIndexType;
import neatlogic.framework.knowledge.crossover.IKnowledgeDocumentCrossoverService;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeTagMapper;
import neatlogic.framework.knowledge.dto.*;
import neatlogic.framework.knowledge.exception.*;
import neatlogic.framework.lcs.linehandler.core.ILineHandler;
import neatlogic.framework.lcs.linehandler.core.LineHandlerFactory;
import neatlogic.framework.util.UuidUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/** 集中处理知识文档的检索、版本权限和草稿事务，供接口及跨模块调用共用。 */
@Service
public class KnowledgeDocumentCrossoverService implements IKnowledgeDocumentCrossoverService {
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeDocumentCrossoverService.class);
    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Resource
    private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;
    @Resource
    private KnowledgeTagMapper knowledgeTagMapper;
    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Override
    public KnowledgeDocumentVersionVo getViewableVersion(Long versionId) throws Exception {
        KnowledgeDocumentVersionVo version = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(versionId);
        if (version == null) {
            throw new KnowledgeDocumentVersionNotFoundException(versionId);
        }
        knowledgeDocumentService.checkViewPermissionByDocumentIdAndVersionId(version.getKnowledgeDocumentId(), versionId);
        return version;
    }

    @Override
    public String getVersionText(Long versionId) throws Exception {
        getViewableVersion(versionId);
        KnowledgeDocumentVo document = knowledgeDocumentService.getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(versionId);
        return knowledgeDocumentService.getContent(document.getLineList());
    }

    @Override
    public boolean isCircleMember(Long circleId) {
        return knowledgeDocumentService.isMember(circleId) == 1;
    }

    @Override
    public JSONObject searchDocuments(JSONObject jsonObj) {
        JSONObject resultJson = new JSONObject();
        JSONArray returnDataList = new JSONArray();
        KnowledgeDocumentVo documentVoParam = new KnowledgeDocumentVo((JSONObject) jsonObj.clone());
        //补充查看权限条件参数（圈子成员or圈子审批人）
        getDocumentViewParam(documentVoParam);
        String knowledgeDocumentTypeUuid = documentVoParam.getKnowledgeDocumentTypeUuid();
        if (StringUtils.isNotBlank(knowledgeDocumentTypeUuid)) {
            KnowledgeDocumentTypeVo typeVo = knowledgeDocumentTypeMapper.getTypeByUuid(documentVoParam.getKnowledgeDocumentTypeUuid());
            if (typeVo == null) {
                throw new KnowledgeDocumentTypeNotFoundException(documentVoParam.getKnowledgeDocumentTypeUuid());
            }
            documentVoParam.setKnowledgeCircleId(typeVo.getKnowledgeCircleId());
            List<KnowledgeDocumentTypeVo> typeList = knowledgeDocumentTypeMapper.getChildAndSelfByLftRht(typeVo.getLft(), typeVo.getRht(), typeVo.getKnowledgeCircleId());
            List<String> typeUuidList = typeList.stream().map(KnowledgeDocumentTypeVo::getUuid).collect(Collectors.toList());
            documentVoParam.setKnowledgeDocumentTypeUuidList(typeUuidList);
        }
        //根据keyword等条件，从数据库搜索知识
        List<KnowledgeDocumentVo> documentList = new ArrayList<>();
        List<Long> documentIdList = knowledgeDocumentMapper.searchKnowledgeDocumentId(documentVoParam);
        if (CollectionUtils.isNotEmpty(documentIdList)) {
            documentList = knowledgeDocumentMapper.getKnowledgeDocumentByIdList(documentIdList);
        }
        //根据keyword等条件，从数据库搜索符合条件的知识count
        Integer total = knowledgeDocumentMapper.searchKnowledgeDocumentIdCount(documentVoParam);
        //一次性查询登录用户的所有点赞知识list和收藏的知识list，提供给后续循环判断知识是否已收藏和点赞
        List<Long> collectedKnowledgeDocumentIdList = new ArrayList<Long>();
        List<Long> favorKnowledgeDocumentIdList = new ArrayList<Long>();
        if (CollectionUtils.isNotEmpty(documentList)) {
            collectedKnowledgeDocumentIdList = knowledgeDocumentMapper.getKnowledgeDocumentCollectDocumentIdListByUserUuidAndDocumentIdList(UserContext.get().getUserUuid(true), documentIdList);
            favorKnowledgeDocumentIdList = knowledgeDocumentMapper.getKnowledgeDocumentFavorDocumentIdListByUserUuidAndDocumentIdList(UserContext.get().getUserUuid(true), documentIdList);
        }
        //判断知识圈是否拥有审批权限。如果入参传入知识全类型，则查询是否有该圈子类型的审批权限，否则一次性获取当前登录人拥有审批权限的所有圈子id
        Integer isApprover = null;
        List<Long> approveCircleIdList = new ArrayList<Long>();
        AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
        if (StringUtils.isNotBlank(documentVoParam.getKnowledgeDocumentTypeUuid())) {
            KnowledgeDocumentTypeVo knowledgeDocumentTypeVo = knowledgeDocumentTypeMapper.getTypeByUuid(documentVoParam.getKnowledgeDocumentTypeUuid());
            if (knowledgeDocumentTypeVo == null) {
                throw new KnowledgeDocumentTypeNotFoundException(documentVoParam.getKnowledgeDocumentTypeUuid());
            }
            isApprover = knowledgeDocumentMapper.checkUserIsApprover(knowledgeDocumentTypeVo.getKnowledgeCircleId(), UserContext.get().getUserUuid(true), authenticationInfoVo.getTeamUuidList(), authenticationInfoVo.getRoleUuidList());
        } else {
            approveCircleIdList = knowledgeDocumentMapper.getUserAllApproverCircleIdList(UserContext.get().getUserUuid(true), authenticationInfoVo.getTeamUuidList(), authenticationInfoVo.getRoleUuidList());
        }
        //获取所有知识当前激活版本
        List<Long> activeVersionIdList = new ArrayList<>();
        for(KnowledgeDocumentVo knowledgeDocumentVo : documentList){
            activeVersionIdList.add(knowledgeDocumentVo.getKnowledgeDocumentVersionId());
        }
        //一次性获取知识搜索关键字最匹配下标信息,提供给后续循环截取内容和高亮关键字
        List<String> keywordList = new ArrayList<>();
        Map<Long, FullTextIndexVo> versionIndexVoMap = new HashMap<>();
        Map<Long,String> versionContentMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(documentVoParam.getKeywordList())) {
            keywordList = new ArrayList<>(documentVoParam.getKeywordList());
        }
        if(CollectionUtils.isNotEmpty(activeVersionIdList)) {
            knowledgeDocumentService.setVersionContentMap(keywordList, activeVersionIdList, versionIndexVoMap, versionContentMap);
        }
        //循环知识，补充额外信息
        for (KnowledgeDocumentVo knowledgeDocumentVo : documentList) {
            knowledgeDocumentVo.setIsEditable(1);
            //判断是否有审批权限，拥有审批权限的人可以删除
            if (isApprover == null) {
                isApprover = 0;
                if (approveCircleIdList.contains(knowledgeDocumentVo.getKnowledgeCircleId())) {
                    isApprover = 1;
                }
            }
            knowledgeDocumentVo.setIsDeletable(isApprover);
            //判断当前登录人是否已经收藏
            if (collectedKnowledgeDocumentIdList.contains(knowledgeDocumentVo.getId())) {
                knowledgeDocumentVo.setIsCollect(1);
            }
            //判断当前登录人是否已经点赞
            if (favorKnowledgeDocumentIdList.contains(knowledgeDocumentVo.getId())) {
                knowledgeDocumentVo.setIsFavor(1);
            }
            //设置标题、截取内容，并高亮
            knowledgeDocumentService.setTitleAndShortcutContentHighlight(keywordList, knowledgeDocumentVo.getKnowledgeDocumentVersionId(),knowledgeDocumentVo, versionIndexVoMap, versionContentMap);
            //组装返回数据
            JSONObject returnData = JSON.parseObject(JSON.toJSONString(knowledgeDocumentVo));
            returnData.put("knowledgeDocumentId", returnData.getLong("id"));
            returnData.put("id", returnData.getLong("knowledgeDocumentVersionId"));
            returnDataList.add(returnData);
        }

        resultJson.put("dataList", returnDataList);
        resultJson.put("rowNum", total);
        resultJson.put("pageSize", documentVoParam.getPageSize());
        resultJson.put("currentPage", documentVoParam.getCurrentPage());
        resultJson.put("pageCount", PageUtil.getPageCount(total, documentVoParam.getPageSize()));
        return resultJson;
    }

    /** 列表使用与详情相同的作者、审核人或圈子成员可见规则。 */
    @Override
    public List<JSONObject> listOrigins(Map<String, List<Long>> origins) {
        List<JSONObject> visible = new ArrayList<>();
        if (origins.isEmpty()) {
            return visible;
        }
        String user = UserContext.get().getUserUuid(true);
        for (JSONObject row : knowledgeDocumentMapper.listOrigins(origins)) {
            if (user.equals(row.getString("lcu")) || user.equals(row.getString("reviewer")) || isCircleMember(row.getLong("circleId"))) {
                row.remove("lcu");
                row.remove("reviewer");
                row.remove("circleId");
                visible.add(row);
            }
        }
        return visible;
    }

    /** 将当前用户、团队和角色加入文档可见范围。 */
    private void getDocumentViewParam(KnowledgeDocumentVo documentVoParam) {
        String userUuid = UserContext.get().getUserUuid(true);
        documentVoParam.setCircleUserUuid(userUuid);

        AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
        documentVoParam.setCircleTeamUuidList(authenticationInfoVo.getTeamUuidList());
        documentVoParam.setCircleRoleUuidList(authenticationInfoVo.getRoleUuidList());
    }

    /** 事务覆盖文档、版本、正文及送审审计，调用方已有事务时与之共同提交。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JSONObject saveDraft(JSONObject jsonObj) throws PermissionDeniedException {
        JSONObject origin = null;
        if (StringUtils.isNotBlank(jsonObj.getString("source"))) {
            Long invokeId = jsonObj.getLong("invokeId");
            if (invokeId == null) throw new ParamNotExistsException("invokeId");
            String source = jsonObj.getString("source");
            // 生成来源必须经其业务入口校验权限和修订号，普通直接来源可使用自己的标识。
            if ("text".equals(source) || KnowledgeGenerationSources.get(source) != null) {
                throw new ParamIrregularException("source", source);
            }
            origin = new JSONObject().fluentPut("source", source).fluentPut("invokeId", invokeId);
        }
        return saveDraft(jsonObj, origin);
    }

    /** 来源由调用服务解析；版本及其创建来源必须同时提交或回滚。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JSONObject saveDraft(JSONObject jsonObj, JSONObject origin) throws PermissionDeniedException {
        Long knowledgeDocumentVersionId = jsonObj.getLong("knowledgeDocumentVersionId");
        KnowledgeDocumentVo documentVo = JSON.toJavaObject(jsonObj, KnowledgeDocumentVo.class);
        KnowledgeDocumentTypeVo knowledgeDocumentTypeVo = knowledgeDocumentTypeMapper.getTypeByUuid(documentVo.getKnowledgeDocumentTypeUuid());
        if (knowledgeDocumentTypeVo == null) {
            throw new KnowledgeDocumentTypeNotFoundException(documentVo.getKnowledgeDocumentTypeUuid());
        }
        if (knowledgeDocumentService.isMember(knowledgeDocumentTypeVo.getKnowledgeCircleId()) == 0) {
            throw new PermissionDeniedException();
        }
        documentVo.setKnowledgeCircleId(knowledgeDocumentTypeVo.getKnowledgeCircleId());
        documentVo.setId(null);
        JSONObject resultObj = new JSONObject();
        Integer isSubmit = jsonObj.getInteger("isSubmit");
        int isReviewable = 0;
        String status = KnowledgeDocumentVersionStatus.DRAFT.getValue();
        if (Objects.equals(isSubmit, 1)) {
            status = KnowledgeDocumentVersionStatus.SUBMITTED.getValue();
            isReviewable = knowledgeDocumentService.isReviewer(knowledgeDocumentTypeVo.getKnowledgeCircleId());
        }
        resultObj.put("isReviewable", isReviewable);

        boolean needSaveDocument = true;
        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = new KnowledgeDocumentVersionVo();
        knowledgeDocumentVersionVo.setTitle(documentVo.getTitle());
        knowledgeDocumentVersionVo.setKnowledgeDocumentTypeUuid(documentVo.getKnowledgeDocumentTypeUuid());
        knowledgeDocumentVersionVo.setLcu(UserContext.get().getUserUuid(true));
        knowledgeDocumentVersionVo.setStatus(status);
        if (knowledgeDocumentVersionId == null) {
            /* 没有版本id，则是首次创建文档 **/
            if (knowledgeDocumentMapper.getKnowledgeDocumentByTitle(documentVo.getTitle()) != null) {
                throw new KnowledgeDocumentTitleRepeatException(documentVo.getTitle());
            }
            documentVo.setFcu(UserContext.get().getUserUuid(true));
            documentVo.setVersion(0);
            knowledgeDocumentMapper.insertKnowledgeDocument(documentVo);
            knowledgeDocumentMapper.insertKnowledgeDocumentViewCount(documentVo.getId(), 0);
            knowledgeDocumentVersionVo.setKnowledgeDocumentId(documentVo.getId());
            knowledgeDocumentVersionVo.setFromVersion(0);
            knowledgeDocumentMapper.insertKnowledgeDocumentVersion(knowledgeDocumentVersionVo);
            documentVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionVo.getId());
            resultObj.put("knowledgeDocumentId", documentVo.getId());
            resultObj.put("knowledgeDocumentVersionId", knowledgeDocumentVersionVo.getId());
        } else {
            /* 有版本id，则是在已有文档上修改 **/
            KnowledgeDocumentVersionVo oldKnowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
            if (oldKnowledgeDocumentVersionVo == null) {
                throw new KnowledgeDocumentVersionNotFoundException(knowledgeDocumentVersionId);
            }
            /* 获取文档锁 **/
            KnowledgeDocumentVo oldDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentLockById(oldKnowledgeDocumentVersionVo.getKnowledgeDocumentId());
            if (oldDocumentVo == null) {
                throw new KnowledgeDocumentNotFoundException(oldKnowledgeDocumentVersionVo.getKnowledgeDocumentId());
            }
            if (status.equals(KnowledgeDocumentVersionStatus.SUBMITTED.getValue())) {
                if (knowledgeDocumentMapper.checkIFThereIsSubmittedDraftByKnowDocumentId(oldKnowledgeDocumentVersionVo.getKnowledgeDocumentId()) > 0) {
                    throw new KnowledgeDocumentDraftSubmitFailedExecption();
                }
            }
            documentVo.setId(oldDocumentVo.getId());
            if(knowledgeDocumentMapper.checkKnowledgeDocumentTitleIsRepeat(documentVo) > 0){
                throw new KnowledgeDocumentTitleRepeatException(documentVo.getTitle());
            }
            resultObj.put("knowledgeDocumentId", documentVo.getId());
            KnowledgeDocumentVo before = knowledgeDocumentService.getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
            oldKnowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
            if (knowledgeDocumentVersionId.equals(oldDocumentVo.getKnowledgeDocumentVersionId())) {
                if (!checkDocumentIsModify(before, documentVo)) {
                    throw new KnowledgeDocumentUnmodifiedCannotBeSavedException();
                }
                /* 如果入参版本id是文档当前版本id，说明该操作是当前版本上修改首次存草稿 **/
                knowledgeDocumentVersionVo.setKnowledgeDocumentId(documentVo.getId());
                knowledgeDocumentVersionVo.setFromVersion(oldKnowledgeDocumentVersionVo.getVersion());
                if (oldKnowledgeDocumentVersionVo.getIsDelete() == 1) {
                    knowledgeDocumentVersionVo.setFromVersion(0);
                } else {
                    knowledgeDocumentVersionVo.setFromVersion(oldKnowledgeDocumentVersionVo.getVersion());
                }
                knowledgeDocumentMapper.insertKnowledgeDocumentVersion(knowledgeDocumentVersionVo);
                documentVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionVo.getId());
                resultObj.put("knowledgeDocumentVersionId", knowledgeDocumentVersionVo.getId());
            } else {
                /* 如果入参版本id不是文档当前版本id，说明该操作是在已有草稿上再次保存 **/
                if (KnowledgeDocumentVersionStatus.PASSED.getValue().equals(oldKnowledgeDocumentVersionVo.getStatus())) {
                    throw new KnowledgeDocumentDraftPublishedCannotBeModifiedException();
                } else if (KnowledgeDocumentVersionStatus.SUBMITTED.getValue().equals(oldKnowledgeDocumentVersionVo.getStatus())) {
                    throw new KnowledgeDocumentDraftSubmittedCannotBeModifiedException();
                }
                if (!oldKnowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid(true))) {
                    throw new KnowledgeDocumentCurrentUserNotOwnerException();
                }
                knowledgeDocumentVersionVo.setId(knowledgeDocumentVersionId);
                documentVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionVo.getId());
                resultObj.put("knowledgeDocumentVersionId", knowledgeDocumentVersionVo.getId());
                needSaveDocument = checkDocumentIsModify(before, documentVo);
                if (needSaveDocument) {
                    knowledgeDocumentMapper.deleteKnowledgeDocumentLineByKnowledgeDocumentVersionId(knowledgeDocumentVersionVo.getId());
                    knowledgeDocumentMapper.deleteKnowledgeDocumentFileByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentFileVo(documentVo.getId(), knowledgeDocumentVersionVo.getId()));
                    knowledgeDocumentMapper.deleteKnowledgeDocumentTagByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentTagVo(documentVo.getId(), knowledgeDocumentVersionVo.getId()));
                }
                /* 覆盖旧草稿时，更新标题、修改用户、修改时间，删除行数据、附件、标签数据，后面再重新插入 **/
                if (oldKnowledgeDocumentVersionVo.getIsDelete() == 1) {
                    knowledgeDocumentVersionVo.setFromVersion(0);
                }
                knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(knowledgeDocumentVersionVo);
            }
        }
        // 新建或派生版本会分配新 ID；覆盖草稿和送审保留最初的来源。
        if (origin != null && !Objects.equals(knowledgeDocumentVersionId, documentVo.getKnowledgeDocumentVersionId())) {
            JSONObject record = (JSONObject) origin.clone();
            record.put("documentId", documentVo.getId());
            record.put("versionId", documentVo.getKnowledgeDocumentVersionId());
            ISyncSource syncSource = SyncSourceFactory.getProvider(origin.getString("source"));
            if (syncSource != null) {
                syncSource.checkConversion(origin.getLong("invokeId"));
            }
            try {
                knowledgeDocumentMapper.insertVersionOrigin(record);
            } catch (DuplicateKeyException ex) {
                logger.error("知识版本创建来源冲突，文档 {}，版本 {}", documentVo.getId(), documentVo.getKnowledgeDocumentVersionId(), ex);
                throw new KnowledgeDocumentRepeatInvokeException(origin.getString("source"), origin.getLong("invokeId"),
                        documentVo.getKnowledgeDocumentVersionId(), ex);
            }
        }
        if (needSaveDocument) {
            saveDocument(documentVo);
            //创建全文检索索引
            IFullTextIndexHandler handler = FullTextIndexHandlerFactory.getHandler(KnowledgeFullTextIndexType.KNOW_DOCUMENT_VERSION);
            if (handler != null) {
                handler.createIndex(documentVo.getKnowledgeDocumentVersionId());
            }
        }
        if (status.equals(KnowledgeDocumentVersionStatus.SUBMITTED.getValue())) {
            knowledgeDocumentService.audit(documentVo.getId(), documentVo.getKnowledgeDocumentVersionId(), KnowledgeDocumentOperate.SUBMIT, null);
        }

        return resultObj;
    }

    /** 保存附件、标签及正文，并更新版本大小。 */
    private void saveDocument(KnowledgeDocumentVo documentVo) {
        /* 保存附件 **/
        KnowledgeDocumentFileVo knowledgeDocumentFileVo = new KnowledgeDocumentFileVo();
        knowledgeDocumentFileVo.setKnowledgeDocumentId(documentVo.getId());
        knowledgeDocumentFileVo.setKnowledgeDocumentVersionId(documentVo.getKnowledgeDocumentVersionId());
        for (Long fileId : documentVo.getFileIdList()) {
            knowledgeDocumentFileVo.setFileId(fileId);
            knowledgeDocumentMapper.insertKnowledgeDocumentFile(knowledgeDocumentFileVo);
        }
        /* 保存标签 **/
        KnowledgeDocumentTagVo knowledgeDocumentTagVo = new KnowledgeDocumentTagVo();
        knowledgeDocumentTagVo.setKnowledgeDocumentId(documentVo.getId());
        knowledgeDocumentTagVo.setKnowledgeDocumentVersionId(documentVo.getKnowledgeDocumentVersionId());
        for (String tagName : documentVo.getTagList()) {
            Long tagId = knowledgeTagMapper.getKnowledgeTagIdByName(tagName);
            if (tagId == null) {
                KnowledgeTagVo knowledgeTagVo = new KnowledgeTagVo(tagName);
                knowledgeTagMapper.insertKnowledgeTag(knowledgeTagVo);
                tagId = knowledgeTagVo.getId();
            }
            knowledgeDocumentTagVo.setTagId(tagId);
            knowledgeDocumentMapper.insertKnowledgeDocumentTag(knowledgeDocumentTagVo);
        }
        /* 保存每行内容，计算文档大小 **/
        int size = 0;
        int lineNumber = 0;
        List<KnowledgeDocumentLineVo> knowledgeDocumentLineList = new ArrayList<>(100);
        for (KnowledgeDocumentLineVo knowledgeDocumentLineVo : documentVo.getLineList()) {
            knowledgeDocumentLineVo.setLineNumber(++lineNumber);
            knowledgeDocumentLineVo.setKnowledgeDocumentId(documentVo.getId());
            knowledgeDocumentLineVo.setKnowledgeDocumentVersionId(documentVo.getKnowledgeDocumentVersionId());
            knowledgeDocumentLineVo.setUuid(UuidUtil.randomUuid());
            if (knowledgeDocumentLineVo.getConfig() != null) {
                KnowledgeDocumentLineConfigVo knowledgeDocumentLineConfigVo = new KnowledgeDocumentLineConfigVo(knowledgeDocumentLineVo.getConfigStr());
                knowledgeDocumentLineVo.setConfigHash(knowledgeDocumentLineConfigVo.getHash());
                if (knowledgeDocumentMapper.checkKnowledgeDocumentLineConfigHashIsExists(knowledgeDocumentLineConfigVo.getHash()) == 0) {
                    knowledgeDocumentMapper.insertKnowledgeDocumentLineConfig(knowledgeDocumentLineConfigVo);
                }
            }
            if (StringUtils.isNotBlank(knowledgeDocumentLineVo.getContent())) {
                size += knowledgeDocumentLineVo.getContent().getBytes(StandardCharsets.UTF_8).length;
                KnowledgeDocumentLineContentVo knowledgeDocumentLineContentVo = new KnowledgeDocumentLineContentVo(knowledgeDocumentLineVo.getContent());
                knowledgeDocumentLineVo.setContentHash(knowledgeDocumentLineContentVo.getHash());
                if (knowledgeDocumentMapper.checkKnowledgeDocumentLineContentHashIsExists(knowledgeDocumentLineContentVo.getHash()) == 0) {
                    knowledgeDocumentMapper.insertKnowledgeDocumentLineContent(knowledgeDocumentLineContentVo);
                }
            }
            knowledgeDocumentLineList.add(knowledgeDocumentLineVo);
            if (knowledgeDocumentLineList.size() >= 100) {
                knowledgeDocumentMapper.insertKnowledgeDocumentLineList(knowledgeDocumentLineList);
                knowledgeDocumentLineList.clear();
            }
        }
        if (CollectionUtils.isNotEmpty(knowledgeDocumentLineList)) {
            knowledgeDocumentMapper.insertKnowledgeDocumentLineList(knowledgeDocumentLineList);
        }
        /* 更新文档大小 **/
        KnowledgeDocumentVersionVo updateSizeVo = new KnowledgeDocumentVersionVo();
        updateSizeVo.setId(documentVo.getKnowledgeDocumentVersionId());
        updateSizeVo.setSize(size);
        knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(updateSizeVo);
    }

    /** 比较分类、标题、附件、标签和逐行正文，判断是否需要重写草稿。 */
    private boolean checkDocumentIsModify(KnowledgeDocumentVo before, KnowledgeDocumentVo after) {
        if (!Objects.equals(before.getKnowledgeDocumentTypeUuid(), after.getKnowledgeDocumentTypeUuid())) {
            return true;
        }
        if (!Objects.equals(before.getTitle(), after.getTitle())) {
            return true;
        }
        if (!SetUtils.isEqualSet(new HashSet<>(before.getFileIdList()), new HashSet<>(after.getFileIdList()))) {
            return true;
        }
        if (!SetUtils.isEqualSet(new HashSet<>(before.getTagList()), new HashSet<>(after.getTagList()))) {
            return true;
        }
        List<KnowledgeDocumentLineVo> beforeLineList = before.getLineList();
        List<KnowledgeDocumentLineVo> afterLineList = after.getLineList();
        if (beforeLineList.size() != afterLineList.size()) {
            return true;
        }
        Iterator<KnowledgeDocumentLineVo> beforeLineIterator = beforeLineList.iterator();
        Iterator<KnowledgeDocumentLineVo> afterLineIterator = afterLineList.iterator();
        while (beforeLineIterator.hasNext() && afterLineIterator.hasNext()) {
            KnowledgeDocumentLineVo beforeLine = beforeLineIterator.next();
            KnowledgeDocumentLineVo afterLine = afterLineIterator.next();
            if (!Objects.equals(beforeLine.getHandler(), afterLine.getHandler())) {
                return true;
            }
            String handler = beforeLine.getHandler();
            ILineHandler lineHandler = LineHandlerFactory.getHandler(handler);
            if (lineHandler == null) {
                return true;
            }
            String beforeMainBody = lineHandler.getMainBody(beforeLine);
            String afterMainBody = lineHandler.getMainBody(afterLine);
            if (!Objects.equals(beforeMainBody, afterMainBody)) {
                return true;
            }
            if (!Objects.equals(beforeLine.getConfigStr(), afterLine.getConfigStr())) {
                return true;
            }
        }
        return false;
    }
}
