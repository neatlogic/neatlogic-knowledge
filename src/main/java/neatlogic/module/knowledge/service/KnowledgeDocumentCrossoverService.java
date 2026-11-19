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
import neatlogic.framework.knowledge.constvalue.KnowledgeSource;
import neatlogic.framework.knowledge.crossover.IKnowledgeDocumentCrossoverService;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentAuditMapper;
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
    private KnowledgeDocumentAuditMapper knowledgeDocumentAuditMapper;
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

    /** 统一版本检索、权限、分页、正文截取及高亮，内部草稿模式保留真实状态。 */
    @Override
    public JSONObject searchDocumentVersions(JSONObject filter, boolean editableDraftOnly) {
        JSONObject resultJson = new JSONObject();
        List<Long> documentVersionIdList = null;
        Integer total = 0;
        KnowledgeDocumentVersionVo documentVersionVoParam = new KnowledgeDocumentVersionVo((JSONObject) filter.clone());
        if (editableDraftOnly) {
            // 作者、状态及成员身份只从服务端产生，业务请求不能扩大候选范围。
            KnowledgeDocumentVo scope = new KnowledgeDocumentVo();
            getDocumentViewParam(scope);
            String category = filter.getString("knowledgeDocumentTypeUuid");
            if (StringUtils.isNotBlank(category)) {
                KnowledgeDocumentTypeVo type = knowledgeDocumentTypeMapper.getTypeByUuid(category);
                if (type == null) throw new KnowledgeDocumentTypeNotFoundException(category);
                scope.setKnowledgeDocumentTypeUuidList(knowledgeDocumentTypeMapper
                        .getChildAndSelfByLftRht(type.getLft(), type.getRht(), type.getKnowledgeCircleId())
                        .stream().map(KnowledgeDocumentTypeVo::getUuid).collect(Collectors.toList()));
            }
            documentVersionVoParam.setEditableDraftScope(scope);
            documentVersionVoParam.setStatusList(new ArrayList<>(List.of(KnowledgeDocumentVersionStatus.DRAFT.getValue())));
            documentVersionVoParam.setLcuList(new ArrayList<>(List.of(scope.getCircleUserUuid())));
            documentVersionVoParam.setReviewerList(null);
            documentVersionVoParam.setCurrentPage(1);
            documentVersionVoParam.setPageSize(Math.max(1, Math.min(100, filter.getIntValue("pageSize"))));
        }
        //拼装 “审批人”条件
        knowledgeDocumentService.getReviewerParam(documentVersionVoParam);
        //根据keyword等条件，查询符合条件的知识版本
        List<String> statusList = documentVersionVoParam.getStatusList();
        // 全部审批状态合并圈子中待审批版本与本人已审批版本。
        if (statusList.contains(KnowledgeDocumentVersionStatus.ALL.getValue())) {
            documentVersionIdList = knowledgeDocumentMapper.getMyAllReviewKnowledgeDocumentVersionIdList(documentVersionVoParam);
            total = knowledgeDocumentMapper.getMyAllReviewKnowledgeDocumentVersionCount(documentVersionVoParam);
        } else {
            documentVersionIdList = knowledgeDocumentMapper.searchKnowledgeDocumentVersionId(documentVersionVoParam);
            total = knowledgeDocumentMapper.searchKnowledgeDocumentVersionIdCount(documentVersionVoParam);
        }
        //根据keyword等条件，从数据库搜索符合条件的知识版本count
        List<KnowledgeDocumentVersionVo> documentVersionList = new ArrayList<KnowledgeDocumentVersionVo>();
        if (CollectionUtils.isNotEmpty(documentVersionIdList)) {
            documentVersionList = knowledgeDocumentMapper.getKnowledgeDocumentVersionByIdList(documentVersionIdList);
        }
        //一次性查出所有分页版本id的不通过原因
        List<Long> failVersionIdList = new ArrayList<>();
        List<Long> versionIdList = new ArrayList<>();
        List<KnowledgeDocumentAuditVo> failAuditList = null;
        for (KnowledgeDocumentVersionVo versionVo : documentVersionList) {
            versionIdList.add(versionVo.getId());
            if (KnowledgeDocumentVersionStatus.REJECTED.getValue().equals(versionVo.getStatus())) {
                failVersionIdList.add(versionVo.getId());
            }
        }
        if (CollectionUtils.isNotEmpty(failVersionIdList)) {
            failAuditList = knowledgeDocumentAuditMapper.getKnowledgeDocumentAuditListByDocumentVersionIdListAndOperate(failVersionIdList, KnowledgeDocumentOperate.REJECT.getValue());
        }
        //一次性获取知识搜索关键字最匹配下标信息,提供给后续循环截取内容和高亮关键字
        Map<Long, FullTextIndexVo> versionIndexVoMap = new HashMap<>();
        Map<Long, String> versionContentMap = new HashMap<>();
        List<String> keywordList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(documentVersionVoParam.getKeywordList())) {
            keywordList = new ArrayList<>(documentVersionVoParam.getKeywordList());
        }
        if (CollectionUtils.isNotEmpty(versionIdList)) {
            knowledgeDocumentService.setVersionContentMap(keywordList, versionIdList, versionIndexVoMap, versionContentMap);
        }
        //循环知识版本，补充额外信息
        for (KnowledgeDocumentVersionVo knowledgeDocumentVersionVo : documentVersionList) {
            // 草稿作者可删除；编辑还需具有圈子权限，内部模式已在分页前校验。
            if (documentVersionVoParam.getStatusList().contains(KnowledgeDocumentVersionStatus.DRAFT.getValue()) && knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid())) {
                //如果不在圈子内则不允许编辑
                AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
                if (editableDraftOnly || knowledgeDocumentMapper.checkUserIsMember(knowledgeDocumentVersionVo.getKnowledgeCircleId(), UserContext.get().getUserUuid(true), authenticationInfoVo.getTeamUuidList(), authenticationInfoVo.getRoleUuidList()) > 0) {
                    knowledgeDocumentVersionVo.setIsEditable(1);
                }
                knowledgeDocumentVersionVo.setIsDeletable(1);
            }
            //如果审核不通过，则补充原因
            if (CollectionUtils.isNotEmpty(failAuditList)) {
                for (KnowledgeDocumentAuditVo failAudit : failAuditList) {
                    if (failAudit.getKnowledgeDocumentVersionId().equals(knowledgeDocumentVersionVo.getId())) {
                        knowledgeDocumentVersionVo.setRejectReason(failAudit.getConfig() == null ? StringUtils.EMPTY:failAudit.getConfig().getString("content"));
                    }
                }
            }
            //设置标题、截取内容，并高亮
            knowledgeDocumentService.setTitleAndShortcutContentHighlight(keywordList, knowledgeDocumentVersionVo.getId(), knowledgeDocumentVersionVo, versionIndexVoMap, versionContentMap);
        }
        //补充状态
        setVersionStatusCount(documentVersionVoParam, resultJson);

        resultJson.put("dataList", documentVersionList);
        resultJson.put("rowNum", total);
        resultJson.put("pageSize", documentVersionVoParam.getPageSize());
        resultJson.put("currentPage", documentVersionVoParam.getCurrentPage());
        resultJson.put("pageCount", PageUtil.getPageCount(total, documentVersionVoParam.getPageSize()));
        return resultJson;
    }

    /** 保留版本搜索原有的审批状态计数及查询分支。 */
    private void setVersionStatusCount(KnowledgeDocumentVersionVo documentVersionVoParam, JSONObject resultJson) {
        if (!documentVersionVoParam.getStatusList().contains(KnowledgeDocumentVersionStatus.DRAFT.getValue())) {
            JSONArray statusArray = new JSONArray();
            JSONObject jsonAll = new JSONObject();
            List<String> statusTmpList = new ArrayList<>();
            statusArray.add(jsonAll);
            JSONObject jsonSubmit = new JSONObject();
            statusArray.add(jsonSubmit);
            //通过
            JSONObject jsonPass = new JSONObject();
            jsonPass.put("value", KnowledgeDocumentVersionStatus.PASSED.getValue());
            jsonPass.put("text", KnowledgeDocumentVersionStatus.PASSED.getText());
            statusTmpList.add(KnowledgeDocumentVersionStatus.PASSED.getValue());
            documentVersionVoParam.setStatusList(statusTmpList);
            documentVersionVoParam.setIsReviewer(1);
            jsonPass.put("count", knowledgeDocumentMapper.searchKnowledgeDocumentVersionIdCount(documentVersionVoParam));
            statusArray.add(jsonPass);
            //不通过
            JSONObject jsonReject = new JSONObject();
            jsonReject.put("value", KnowledgeDocumentVersionStatus.REJECTED.getValue());
            jsonReject.put("text", KnowledgeDocumentVersionStatus.REJECTED.getText());
            statusTmpList.clear();
            statusTmpList.add(KnowledgeDocumentVersionStatus.REJECTED.getValue());
            documentVersionVoParam.setStatusList(statusTmpList);
            documentVersionVoParam.setIsReviewer(1);
            jsonReject.put("count", knowledgeDocumentMapper.searchKnowledgeDocumentVersionIdCount(documentVersionVoParam));
            statusArray.add(jsonReject);
            //待审批
            jsonSubmit.put("value", KnowledgeDocumentVersionStatus.SUBMITTED.getValue());
            jsonSubmit.put("text", KnowledgeDocumentVersionStatus.SUBMITTED.getText());
            statusTmpList.clear();
            statusTmpList.add(KnowledgeDocumentVersionStatus.SUBMITTED.getValue());
            documentVersionVoParam.setStatusList(statusTmpList);
            knowledgeDocumentService.getReviewerParam(documentVersionVoParam);
            jsonSubmit.put("count", knowledgeDocumentMapper.searchKnowledgeDocumentVersionIdCount(documentVersionVoParam));
            jsonAll.put("value", KnowledgeDocumentVersionStatus.ALL.getValue());
            jsonAll.put("text", KnowledgeDocumentVersionStatus.ALL.getText());
            jsonAll.put("count", jsonSubmit.getLong("count") + jsonPass.getLong("count") + jsonReject.getLong("count"));
            resultJson.put("statusList", statusArray);
        }
    }


    /** 按文档聚合，同一知识存在修订草稿时，阅读仍指向激活正文。 */
    @Override
    public List<JSONObject> getReferenceDocuments(List<Long> documentIds) {
        if (documentIds.isEmpty()) return Collections.emptyList();
        String user = UserContext.get().getUserUuid(true);
        Map<Long, Boolean> membership = new HashMap<>();
        Map<Long, JSONObject> result = new LinkedHashMap<>();
        for (JSONObject row : knowledgeDocumentMapper.listReferenceVersions(documentIds)) {
            boolean own = user.equals(row.getString("lcu"));
            boolean active = Objects.equals(row.getLong("activeId"), row.getLong("versionId")) && "passed".equals(row.getString("status"));
            boolean member = membership.computeIfAbsent(row.getLong("circleId"), this::isCircleMember);
            if (!own && !user.equals(row.getString("reviewer")) && !member) continue;
            Long id = row.getLong("documentId");
            JSONObject doc = result.computeIfAbsent(id, k -> new JSONObject().fluentPut("documentId", id));
            if (row.getBooleanValue("hasPending")) doc.put("pending", true);
            if ("submitted".equals(row.getString("status")) && !doc.containsKey("pendingId")) {
                doc.put("pendingId", row.getLong("versionId"));
                doc.put("pendingTitle", row.getString("title"));
            }
            if (own && "draft".equals(row.getString("status")) && !doc.containsKey("draftId")) {
                doc.put("draftId", row.getLong("versionId"));
                doc.put("draftTitle", row.getString("title"));
            }
            // 阅读优先指向激活版本；尚未发布时展示待审核稿或自己的草稿。
            boolean preferred = active || !doc.containsKey("versionId");
            if (!doc.getBooleanValue("published") && !"submitted".equals(doc.getString("status"))) {
                preferred |= "submitted".equals(row.getString("status")) || Objects.equals(doc.getLong("draftId"), row.getLong("versionId"));
            }
            if (preferred) {
                doc.put("member", member);
                for (String key : Arrays.asList("versionId", "title", "status", "knowledgeDocumentTypeUuid", "documentTypePath", "fromVersion")) doc.put(key, row.get(key));
                doc.put("published", active);
            }
        }
        for (JSONObject doc : result.values()) {
            // 引用展示阅读状态，补充文档优先展示待审核稿或本人草稿。两者沿用知识库的状态色。
            String status = doc.getString("status");
            String editStatus = doc.getBooleanValue("pending") ? KnowledgeDocumentVersionStatus.SUBMITTED.getValue()
                    : doc.containsKey("draftId") ? KnowledgeDocumentVersionStatus.DRAFT.getValue() : status;
            doc.put("statusColor", KnowledgeDocumentVersionStatus.getColor(status));
            doc.put("editStatusColor", KnowledgeDocumentVersionStatus.getColor(editStatus));
            doc.put("editable", doc.getBooleanValue("member") && !doc.getBooleanValue("pending")
                    && (doc.getBooleanValue("published") || doc.get("draftId") != null));
            doc.remove("member");
        }
        return new ArrayList<>(result.values());
    }

    /** 文档锁与知识版本保存及删除共用，避免检查引用后又有其他工单新增引用。 */
    @Override
    public void lockReferenceDocument(Long documentId) {
        if (knowledgeDocumentMapper.getKnowledgeDocumentLockById(documentId) == null) throw new KnowledgeDocumentNotFoundException(documentId);
    }

    /** 通过既有版本删除服务再次校验作者；工单只能请求删除草稿。 */
    @Override
    public void deleteReferenceDraft(Long documentId, Long draftId) throws Exception {
        lockReferenceDocument(documentId);
        KnowledgeDocumentVersionVo version = getViewableVersion(draftId);
        if (!documentId.equals(version.getKnowledgeDocumentId()) || !"draft".equals(version.getStatus())) {
            throw new ParamIrregularException("draftId", String.valueOf(draftId));
        }
        knowledgeDocumentService.deleteVersion(draftId);
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
            // 工单来源由业务入口校验权限并建立关联，即使未安装商业模块也禁止客户端自行指定。
            if (KnowledgeSource.PROCESSTASK.getValue().equalsIgnoreCase(source.trim())
                    || "text".equals(source) || KnowledgeGenerationSources.get(source) != null) {
                throw new ParamIrregularException("source", source);
            }
            origin = new JSONObject().fluentPut("source", source).fluentPut("invokeId", invokeId);
        }
        return saveDraft(jsonObj, origin, true);
    }

    /** 来源由调用服务解析；版本及其创建来源必须同时提交或回滚。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JSONObject saveDraft(JSONObject jsonObj, JSONObject origin) throws PermissionDeniedException {
        return saveDraft(jsonObj, origin, false);
    }

    /** 直接保存的来源由客户端声明，需调用原来源转换校验；业务服务传入的来源已经在其事务内鉴权。 */
    private JSONObject saveDraft(JSONObject jsonObj, JSONObject origin, boolean checkDirectConversion) throws PermissionDeniedException {
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
            // 锁内校验继续编辑的预期状态，防止已发布草稿被当作新的修订起点。
            String expectedStatus = jsonObj.getString("expectedStatus");
            if (expectedStatus != null && (!KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(expectedStatus)
                    || !expectedStatus.equals(oldKnowledgeDocumentVersionVo.getStatus())
                    || Objects.equals(oldKnowledgeDocumentVersionVo.getIsDelete(), 1))) {
                throw new KnowledgeDocumentVersionStateChangedException(knowledgeDocumentVersionId, expectedStatus,
                        oldKnowledgeDocumentVersionVo.getStatus(), oldKnowledgeDocumentVersionVo.getIsDelete());
            }
            // 首次发布前文档指针也可能指向草稿，仍须覆盖同一草稿。
            if (knowledgeDocumentVersionId.equals(oldDocumentVo.getKnowledgeDocumentVersionId())
                    && !KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(oldKnowledgeDocumentVersionVo.getStatus())) {
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
            if (checkDirectConversion && syncSource != null) {
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
