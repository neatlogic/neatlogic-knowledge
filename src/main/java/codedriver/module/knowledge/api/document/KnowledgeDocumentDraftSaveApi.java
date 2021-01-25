package codedriver.module.knowledge.api.document;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.label.NO_AUTH;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.UuidUtil;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentLineHandler;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeTagMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentFileVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentInvokeVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineConfigVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineContentVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentTagVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentTypeVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.dto.KnowledgeTagVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentCurrentUserNotOwnerException;
import codedriver.module.knowledge.exception.KnowledgeDocumentDraftPublishedCannotBeModifiedException;
import codedriver.module.knowledge.exception.KnowledgeDocumentDraftSubmittedCannotBeModifiedException;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import codedriver.module.knowledge.exception.KnowledgeDocumentRepeatInvokeException;
import codedriver.module.knowledge.exception.KnowledgeDocumentTitleRepeatException;
import codedriver.module.knowledge.exception.KnowledgeDocumentTypeNotFoundException;
import codedriver.module.knowledge.exception.KnowledgeDocumentUnmodifiedCannotBeSavedException;
import codedriver.module.knowledge.exception.KnowledgeDocumentVersionNotFoundException;
import codedriver.module.knowledge.service.KnowledgeDocumentService;

@Service
@AuthAction(action = NO_AUTH.class)
@OperationType(type = OperationTypeEnum.CREATE)
@Transactional
public class KnowledgeDocumentDraftSaveApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;
    @Autowired
    private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;
    @Autowired
    private KnowledgeTagMapper knowledgeTagMapper;
    @Autowired
    private TeamMapper teamMapper;

    @Override
    public String getToken() {
        return "knowledge/document/draft/save";
    }

    @Override
    public String getName() {
        return "保存文档草稿";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "knowledgeDocumentVersionId", type = ApiParamType.LONG, desc = "版本id"),
            @Param(name = "knowledgeDocumentTypeUuid", type = ApiParamType.STRING, isRequired = true, desc = "类型id"),
            @Param(name = "title", type = ApiParamType.STRING, isRequired = true, minLength = 1, desc = "标题"),
            @Param(name = "lineList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "行数据列表"),
            @Param(name = "fileIdList", type = ApiParamType.JSONARRAY, desc = "附件id列表"),
            @Param(name = "tagList", type = ApiParamType.JSONARRAY, desc = "标签列表"),
            @Param(name = "invokeId", type = ApiParamType.LONG, desc = "调用者id"),
            @Param(name = "source", type = ApiParamType.STRING, desc = "来源"),
            @Param(name = "isSubmit", type = ApiParamType.INTEGER, desc = "是否提交")
    })
    @Output({
            @Param(name = "knowledgeDocumentId", type = ApiParamType.LONG, desc = "文档id"),
            @Param(name = "knowledgeDocumentVersionId", type = ApiParamType.LONG, desc = "版本id"),
            @Param(name = "isReviewable", type = ApiParamType.INTEGER, desc = "是否能审批"),
    })
    @Description(desc = "保存文档草稿")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long knowledgeDocumentVersionId = jsonObj.getLong("knowledgeDocumentVersionId");
        KnowledgeDocumentVo documentVo = JSON.toJavaObject(jsonObj, KnowledgeDocumentVo.class);
        KnowledgeDocumentTypeVo knowledgeDocumentTypeVo = knowledgeDocumentTypeMapper.getTypeByUuid(documentVo.getKnowledgeDocumentTypeUuid());
        if (knowledgeDocumentTypeVo == null) {
            throw new KnowledgeDocumentTypeNotFoundException(documentVo.getKnowledgeDocumentTypeUuid());
        }
        List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(UserContext.get().getUserUuid(true));
        if (knowledgeDocumentMapper.checkUserIsMember(knowledgeDocumentTypeVo.getKnowledgeCircleId(), UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList()) == 0) {
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
            isReviewable = knowledgeDocumentMapper.checkUserIsApprover(knowledgeDocumentTypeVo.getKnowledgeCircleId(), UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList());
        }
        resultObj.put("isReviewable", isReviewable);

        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = new KnowledgeDocumentVersionVo();
        knowledgeDocumentVersionVo.setTitle(documentVo.getTitle());
        knowledgeDocumentVersionVo.setKnowledgeDocumentTypeUuid(documentVo.getKnowledgeDocumentTypeUuid());
        knowledgeDocumentVersionVo.setLcu(UserContext.get().getUserUuid(true));
        knowledgeDocumentVersionVo.setStatus(status);
        if (knowledgeDocumentVersionId == null) {
            /** 没有版本id，则是首次创建文档 **/
            if (knowledgeDocumentMapper.getKnowledgeDocumentByTitle(documentVo.getTitle()) != null) {
                throw new KnowledgeDocumentTitleRepeatException(documentVo.getTitle());
            }
            documentVo.setFcu(UserContext.get().getUserUuid(true));
            documentVo.setVersion(0);
            knowledgeDocumentMapper.insertKnowledgeDocument(documentVo);
            if (StringUtils.isNotBlank(documentVo.getSource())) {
                if (documentVo.getInvokeId() == null) {
                    throw new ParamNotExistsException("参数：“invokeId”不能为空");
                }
                KnowledgeDocumentInvokeVo knowledgeDocumentInvokeVo = new KnowledgeDocumentInvokeVo(documentVo.getId(), documentVo.getInvokeId(), documentVo.getSource());
                if (knowledgeDocumentMapper.getKnowledgeDocumentIdByInvokeIdAndSource(knowledgeDocumentInvokeVo) != null) {
                    throw new KnowledgeDocumentRepeatInvokeException();
                }
                knowledgeDocumentMapper.insertKnowledgeDocumentInvoke(knowledgeDocumentInvokeVo);
            }
            knowledgeDocumentMapper.insertKnowledgeDocumentViewCount(documentVo.getId(), 0);
            knowledgeDocumentVersionVo.setKnowledgeDocumentId(documentVo.getId());
            knowledgeDocumentVersionVo.setFromVersion(0);
            knowledgeDocumentMapper.insertKnowledgeDocumentVersion(knowledgeDocumentVersionVo);
            documentVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionVo.getId());
            resultObj.put("knowledgeDocumentId", documentVo.getId());
            resultObj.put("knowledgeDocumentVersionId", knowledgeDocumentVersionVo.getId());
        } else {
            /** 有版本id，则是在已有文档上修改 **/
            KnowledgeDocumentVersionVo oldKnowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
            if (oldKnowledgeDocumentVersionVo == null) {
                throw new KnowledgeDocumentVersionNotFoundException(knowledgeDocumentVersionId);
            }
            if (!oldKnowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid(true))) {
                throw new KnowledgeDocumentCurrentUserNotOwnerException();
            }
            /** 获取文档锁 **/
            KnowledgeDocumentVo oldDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentLockById(oldKnowledgeDocumentVersionVo.getKnowledgeDocumentId());
            if (oldDocumentVo == null) {
                throw new KnowledgeDocumentNotFoundException(oldKnowledgeDocumentVersionVo.getKnowledgeDocumentId());
            }
            documentVo.setId(oldDocumentVo.getId());
            resultObj.put("knowledgeDocumentId", documentVo.getId());
            KnowledgeDocumentVo before = knowledgeDocumentService.getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
            oldKnowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
            if (knowledgeDocumentVersionId.equals(oldDocumentVo.getKnowledgeDocumentVersionId())) {
                if (!checkDocumentIsModify(before, documentVo)) {
                    throw new KnowledgeDocumentUnmodifiedCannotBeSavedException();
                }
                /** 如果入参版本id是文档当前版本id，说明该操作是当前版本上修改首次存草稿 **/
                knowledgeDocumentVersionVo.setKnowledgeDocumentId(documentVo.getId());
                knowledgeDocumentVersionVo.setFromVersion(oldKnowledgeDocumentVersionVo.getVersion());
                if(oldKnowledgeDocumentVersionVo.getIsDelete() == 1){
                    knowledgeDocumentVersionVo.setFromVersion(0);
                }else{
                    knowledgeDocumentVersionVo.setFromVersion(oldKnowledgeDocumentVersionVo.getVersion());
                }
                knowledgeDocumentMapper.insertKnowledgeDocumentVersion(knowledgeDocumentVersionVo);
                documentVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionVo.getId());
                resultObj.put("knowledgeDocumentVersionId", knowledgeDocumentVersionVo.getId());
            } else {
                /** 如果入参版本id不是文档当前版本id，说明该操作是在已有草稿上再次保存 **/
                if (KnowledgeDocumentVersionStatus.PASSED.getValue().equals(oldKnowledgeDocumentVersionVo.getStatus())) {
                    throw new KnowledgeDocumentDraftPublishedCannotBeModifiedException();
                } else if (KnowledgeDocumentVersionStatus.SUBMITTED.getValue().equals(oldKnowledgeDocumentVersionVo.getStatus())) {
                    throw new KnowledgeDocumentDraftSubmittedCannotBeModifiedException();
                }
                knowledgeDocumentVersionVo.setId(knowledgeDocumentVersionId);
                documentVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionVo.getId());
                resultObj.put("knowledgeDocumentVersionId", knowledgeDocumentVersionVo.getId());
                if (!checkDocumentIsModify(before, documentVo)) {
                    return resultObj;
                }
                /** 覆盖旧草稿时，更新标题、修改用户、修改时间，删除行数据、附件、标签数据，后面再重新插入 **/
                if(oldKnowledgeDocumentVersionVo.getIsDelete() == 1){
                    knowledgeDocumentVersionVo.setFromVersion(0);
                }
                knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(knowledgeDocumentVersionVo);
                knowledgeDocumentMapper.deleteKnowledgeDocumentLineByKnowledgeDocumentVersionId(knowledgeDocumentVersionVo.getId());
                knowledgeDocumentMapper.deleteKnowledgeDocumentFileByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentFileVo(documentVo.getId(), knowledgeDocumentVersionVo.getId()));
                knowledgeDocumentMapper.deleteKnowledgeDocumentTagByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentTagVo(documentVo.getId(), knowledgeDocumentVersionVo.getId()));
            }
        }
        saveDocument(documentVo);
        return resultObj;
    }
    /**
     * @Description: 保存文档内容
     * @Author: linbq
     * @Date: 2021/1/25 12:14
     * @Params:[documentVo]
     * @Returns:void
     **/
    private void saveDocument(KnowledgeDocumentVo documentVo) throws UnsupportedEncodingException {
        /** 保存附件 **/
        KnowledgeDocumentFileVo knowledgeDocumentFileVo = new KnowledgeDocumentFileVo();
        knowledgeDocumentFileVo.setKnowledgeDocumentId(documentVo.getId());
        knowledgeDocumentFileVo.setKnowledgeDocumentVersionId(documentVo.getKnowledgeDocumentVersionId());
        for (Long fileId : documentVo.getFileIdList()) {
            knowledgeDocumentFileVo.setFileId(fileId);
            knowledgeDocumentMapper.insertKnowledgeDocumentFile(knowledgeDocumentFileVo);
        }
        /** 保存标签 **/
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
        /** 保存每行内容，计算文档大小 **/
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
                size += knowledgeDocumentLineVo.getContent().getBytes("UTF-8").length;
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
        /** 更新文档大小 **/
        KnowledgeDocumentVersionVo updateSizeVo = new KnowledgeDocumentVersionVo();
        updateSizeVo.setId(documentVo.getKnowledgeDocumentVersionId());
        updateSizeVo.setSize(size);
        knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(updateSizeVo);
    }

    /**
     * @param before
     * @param after
     * @return boolean
     * @Time:2020年10月29日
     * @Description: 检查文档内容信息是否被修改
     */
    private boolean checkDocumentIsModify(KnowledgeDocumentVo before, KnowledgeDocumentVo after) {
        //knowledgeDocumentTypeUuid
        if (!Objects.equals(before.getKnowledgeDocumentTypeUuid(), after.getKnowledgeDocumentTypeUuid())) {
            return true;
        }
        //title
        if (!Objects.equals(before.getTitle(), after.getTitle())) {
            return true;
        }
        //fileIdList
        if (!SetUtils.isEqualSet(new HashSet<>(before.getFileIdList()), new HashSet<>(after.getFileIdList()))) {
            return true;
        }
        //tagList
        if (!SetUtils.isEqualSet(new HashSet<>(before.getTagList()), new HashSet<>(after.getTagList()))) {
            return true;
        }
        //lineList
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
            String beforeMainBody = KnowledgeDocumentLineHandler.getMainBody(beforeLine);
            String afterMainBody = KnowledgeDocumentLineHandler.getMainBody(afterLine);
            if (!Objects.equals(beforeMainBody, afterMainBody)) {
//                System.out.println(beforeMainBody);
//                System.out.println(afterMainBody);
                return true;
            }
        }
        return false;
    }
}
