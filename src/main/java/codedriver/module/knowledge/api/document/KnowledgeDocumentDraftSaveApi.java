package codedriver.module.knowledge.api.document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

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
import codedriver.framework.reminder.core.OperationTypeEnum;
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
import codedriver.module.knowledge.exception.KnowledgeDocumentCurrentUserNotMemberException;
import codedriver.module.knowledge.exception.KnowledgeDocumentCurrentUserNotOwnerException;
import codedriver.module.knowledge.exception.KnowledgeDocumentDraftPublishedCannotBeModifiedException;
import codedriver.module.knowledge.exception.KnowledgeDocumentDraftSubmittedCannotBeModifiedException;
import codedriver.module.knowledge.exception.KnowledgeDocumentHasBeenDeletedException;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotCurrentVersionException;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import codedriver.module.knowledge.exception.KnowledgeDocumentTitleRepeatException;
import codedriver.module.knowledge.exception.KnowledgeDocumentTypeNotFoundException;
import codedriver.module.knowledge.exception.KnowledgeDocumentUnmodifiedCannotBeSavedException;
import codedriver.module.knowledge.exception.KnowledgeDocumentVersionNotFoundException;
import codedriver.module.knowledge.service.KnowledgeDocumentService;
@Service
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
        @Param(name = "isSubmit", type = ApiParamType.INTEGER, desc ="是否提交")
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
        if(knowledgeDocumentTypeVo == null) {
            throw new KnowledgeDocumentTypeNotFoundException(documentVo.getKnowledgeDocumentTypeUuid());
        }
        List<String> teamUuidList= teamMapper.getTeamUuidListByUserUuid(UserContext.get().getUserUuid(true));
        if(knowledgeDocumentMapper.checkUserIsMember(knowledgeDocumentTypeVo.getKnowledgeCircleId(), UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList()) == 0) {
            throw new KnowledgeDocumentCurrentUserNotMemberException();
        }
        documentVo.setKnowledgeCircleId(knowledgeDocumentTypeVo.getKnowledgeCircleId());
        documentVo.setId(null);
        JSONObject resultObj = new JSONObject();
        Long documentId = null;
        Long drafrVersionId = null;
        Integer isSubmit = jsonObj.getInteger("isSubmit");
        int isReviewable = 0;
        String status = KnowledgeDocumentVersionStatus.DRAFT.getValue();
        if(Objects.equals(isSubmit, 1)) {
            status = KnowledgeDocumentVersionStatus.SUBMITTED.getValue();
            isReviewable = knowledgeDocumentMapper.checkUserIsApprover(UserContext.get().getUserUuid(true), knowledgeDocumentTypeVo.getKnowledgeCircleId());           
        }
        resultObj.put("isReviewable", isReviewable);
        if(knowledgeDocumentVersionId != null) {
            /** 有版本id，则是在已有文档上修改 **/
            KnowledgeDocumentVersionVo oldKnowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
            if(oldKnowledgeDocumentVersionVo == null) {
                throw new KnowledgeDocumentVersionNotFoundException(knowledgeDocumentVersionId);
            }
            /** 获取文档锁 **/
            knowledgeDocumentMapper.getKnowledgeDocumentLockById(oldKnowledgeDocumentVersionVo.getKnowledgeDocumentId());
            KnowledgeDocumentVo oldDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(oldKnowledgeDocumentVersionVo.getKnowledgeDocumentId());
            if(oldDocumentVo == null) {
                throw new KnowledgeDocumentNotFoundException(oldKnowledgeDocumentVersionVo.getKnowledgeDocumentId());
            }
            if(Objects.equals(oldDocumentVo.getIsDelete(), 1)) {
                throw new KnowledgeDocumentHasBeenDeletedException(oldDocumentVo.getId());
            }
            documentId = oldDocumentVo.getId();
            oldKnowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
            if(knowledgeDocumentVersionId.equals(oldDocumentVo.getKnowledgeDocumentVersionId())) {
                KnowledgeDocumentVo before = knowledgeDocumentService.getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
                if(!checkDocumentIsModify(before, documentVo)) {
                    throw new KnowledgeDocumentUnmodifiedCannotBeSavedException();
                }
                /** 删除这个文档当前用户的草稿 **/
                knowledgeDocumentMapper.deleteKnowledgeDocumentDraftByKnowledgeDocumentIdAndLcu(documentId, UserContext.get().getUserUuid(true));
                /** 如果入参版本id是文档当前版本id，说明该操作是当前版本上修改首次存草稿 **/
                KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = new KnowledgeDocumentVersionVo();
                knowledgeDocumentVersionVo.setTitle(documentVo.getTitle());
                knowledgeDocumentVersionVo.setKnowledgeDocumentTypeUuid(documentVo.getKnowledgeDocumentTypeUuid());
                knowledgeDocumentVersionVo.setKnowledgeDocumentId(documentId);
                knowledgeDocumentVersionVo.setFromVersion(oldKnowledgeDocumentVersionVo.getVersion());
                knowledgeDocumentVersionVo.setLcu(UserContext.get().getUserUuid(true));
                knowledgeDocumentVersionVo.setStatus(status);
                knowledgeDocumentMapper.insertKnowledgeDocumentVersion(knowledgeDocumentVersionVo);
                drafrVersionId = knowledgeDocumentVersionVo.getId();
            }else {
                if(!Objects.equals(oldDocumentVo.getVersion(), oldKnowledgeDocumentVersionVo.getFromVersion())) {
                    throw new KnowledgeDocumentNotCurrentVersionException(oldKnowledgeDocumentVersionVo.getFromVersion());
                }
                /** 如果入参版本id不是文档当前版本id，说明该操作是在已有草稿上再次保存 **/
                if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(oldKnowledgeDocumentVersionVo.getStatus())) {
                    throw new KnowledgeDocumentDraftPublishedCannotBeModifiedException();
                }else if(KnowledgeDocumentVersionStatus.SUBMITTED.getValue().equals(oldKnowledgeDocumentVersionVo.getStatus())) {
                    throw new KnowledgeDocumentDraftSubmittedCannotBeModifiedException();
                }
//                else if(KnowledgeDocumentVersionStatus.EXPIRED.getValue().equals(oldKnowledgeDocumentVersionVo.getStatus())) {
//                    throw new KnowledgeDocumentDraftExpiredCannotBeModifiedException();
//                }
                drafrVersionId = knowledgeDocumentVersionId;
                KnowledgeDocumentVo before = knowledgeDocumentService.getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
                if(!before.getLcu().equals(UserContext.get().getUserUuid(true))) {
                    throw new KnowledgeDocumentCurrentUserNotOwnerException();
                }
                if(!checkDocumentIsModify(before, documentVo)) {
                    resultObj.put("knowledgeDocumentId", documentId);
                    resultObj.put("knowledgeDocumentVersionId", drafrVersionId);
                    return resultObj;
                }
                /** 覆盖旧草稿时，更新标题、修改用户、修改时间，删除行数据、附件、标签数据，后面再重新插入 **/
                KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = new KnowledgeDocumentVersionVo();
                knowledgeDocumentVersionVo.setId(drafrVersionId);
                knowledgeDocumentVersionVo.setKnowledgeDocumentTypeUuid(documentVo.getKnowledgeDocumentTypeUuid());
                knowledgeDocumentVersionVo.setTitle(documentVo.getTitle());
                knowledgeDocumentVersionVo.setStatus(status);
                knowledgeDocumentVersionVo.setLcu(UserContext.get().getUserUuid(true));
                knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(knowledgeDocumentVersionVo);
                knowledgeDocumentMapper.deleteKnowledgeDocumentLineByKnowledgeDocumentVersionId(drafrVersionId);
                knowledgeDocumentMapper.deleteKnowledgeDocumentFileByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentFileVo(documentId, drafrVersionId));
                knowledgeDocumentMapper.deleteKnowledgeDocumentTagByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentTagVo(documentId, drafrVersionId));
            }
        }else {
            /** 没有版本id，则是首次创建文档 **/
            if(knowledgeDocumentMapper.getKnowledgeDocumentByTitle(documentVo.getTitle()) != null) {
                throw new KnowledgeDocumentTitleRepeatException(documentVo.getTitle());
            }
            KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = new KnowledgeDocumentVersionVo();
            documentVo.setFcu(UserContext.get().getUserUuid(true));
            documentVo.setVersion(0);
            knowledgeDocumentMapper.insertKnowledgeDocument(documentVo);
            if(StringUtils.isNotBlank(documentVo.getSource())) {
                if(documentVo.getInvokeId() == null) {
                    throw new ParamNotExistsException("参数：“invokeId”不能为空");
                }
                knowledgeDocumentMapper.insertKnowledgeDocumentInvoke(new KnowledgeDocumentInvokeVo(documentVo.getId(), documentVo.getInvokeId(), documentVo.getSource()));
            }
            knowledgeDocumentMapper.insertKnowledgeDocumentViewCount(documentVo.getId(), 0);
            knowledgeDocumentVersionVo.setTitle(documentVo.getTitle());
            knowledgeDocumentVersionVo.setKnowledgeDocumentTypeUuid(documentVo.getKnowledgeDocumentTypeUuid());
            knowledgeDocumentVersionVo.setKnowledgeDocumentId(documentVo.getId());
            knowledgeDocumentVersionVo.setFromVersion(0);
            knowledgeDocumentVersionVo.setLcu(UserContext.get().getUserUuid(true));
            knowledgeDocumentVersionVo.setStatus(status);
            knowledgeDocumentMapper.insertKnowledgeDocumentVersion(knowledgeDocumentVersionVo);
            documentId = documentVo.getId();
            drafrVersionId = knowledgeDocumentVersionVo.getId();
        }
        /** 保存附件 **/
        KnowledgeDocumentFileVo knowledgeDocumentFileVo = new KnowledgeDocumentFileVo();
        knowledgeDocumentFileVo.setKnowledgeDocumentId(documentId);
        knowledgeDocumentFileVo.setKnowledgeDocumentVersionId(drafrVersionId);
        for(Long fileId : documentVo.getFileIdList()) {
            knowledgeDocumentFileVo.setFileId(fileId);
            knowledgeDocumentMapper.insertKnowledgeDocumentFile(knowledgeDocumentFileVo);
        }
        /** 保存标签 **/
        KnowledgeDocumentTagVo knowledgeDocumentTagVo = new KnowledgeDocumentTagVo();
        knowledgeDocumentTagVo.setKnowledgeDocumentId(documentId);
        knowledgeDocumentTagVo.setKnowledgeDocumentVersionId(drafrVersionId);
        for(String tagName : documentVo.getTagList()) {
            Long tagId = knowledgeTagMapper.getKnowledgeTagIdByName(tagName);
            if(tagId == null) {
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
        List<KnowledgeDocumentLineVo> knowledgeDocumentLineList = new ArrayList<>();
        for(KnowledgeDocumentLineVo knowledgeDocumentLineVo : documentVo.getLineList()) {
            knowledgeDocumentLineVo.setLineNumber(++lineNumber);
            knowledgeDocumentLineVo.setKnowledgeDocumentId(documentId);
            knowledgeDocumentLineVo.setKnowledgeDocumentVersionId(drafrVersionId);
            knowledgeDocumentLineVo.setUuid(UuidUtil.randomUuid());
            if(knowledgeDocumentLineVo.getConfig() != null) {
                KnowledgeDocumentLineConfigVo knowledgeDocumentLineConfigVo = new KnowledgeDocumentLineConfigVo(knowledgeDocumentLineVo.getConfigStr());
                knowledgeDocumentLineVo.setConfigHash(knowledgeDocumentLineConfigVo.getHash());
                if(knowledgeDocumentMapper.checkKnowledgeDocumentLineConfigHashIsExists(knowledgeDocumentLineConfigVo.getHash()) == 0) {
                    knowledgeDocumentMapper.insertKnowledgeDocumentLineConfig(knowledgeDocumentLineConfigVo);
                }
            }
            if(StringUtils.isNotBlank(knowledgeDocumentLineVo.getContent())) {
                size += knowledgeDocumentLineVo.getContent().getBytes("UTF-8").length;
                KnowledgeDocumentLineContentVo knowledgeDocumentLineContentVo = new KnowledgeDocumentLineContentVo(knowledgeDocumentLineVo.getContent());
                knowledgeDocumentLineVo.setContentHash(knowledgeDocumentLineContentVo.getHash());
                if(knowledgeDocumentMapper.checkKnowledgeDocumentLineContentHashIsExists(knowledgeDocumentLineContentVo.getHash()) == 0) {
                    knowledgeDocumentMapper.insertKnowledgeDocumentLineContent(knowledgeDocumentLineContentVo);
                }
            }
            knowledgeDocumentLineList.add(knowledgeDocumentLineVo);
            if(knowledgeDocumentLineList.size() >= 100) {
                knowledgeDocumentMapper.insertKnowledgeDocumentLineList(knowledgeDocumentLineList);
            }
        }
        if(CollectionUtils.isNotEmpty(knowledgeDocumentLineList)) {
            knowledgeDocumentMapper.insertKnowledgeDocumentLineList(knowledgeDocumentLineList);
        }
        /** 更新文档大小 **/
        KnowledgeDocumentVersionVo updateSizeVo = new KnowledgeDocumentVersionVo();
        updateSizeVo.setId(drafrVersionId);
        updateSizeVo.setSize(size);
        knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(updateSizeVo);
        resultObj.put("knowledgeDocumentId", documentId);
        resultObj.put("knowledgeDocumentVersionId", drafrVersionId);
        return resultObj;
    }

    /**
     * 
    * @Time:2020年10月29日
    * @Description: 检查文档内容信息是否被修改 
    * @param before
    * @param after
    * @return boolean
     */
    private boolean checkDocumentIsModify(KnowledgeDocumentVo before, KnowledgeDocumentVo after) {
        //knowledgeDocumentTypeUuid
        if(!Objects.equals(before.getKnowledgeDocumentTypeUuid(), after.getKnowledgeDocumentTypeUuid())) {
            return true;
        }
        //title
        if(!Objects.equals(before.getTitle(), after.getTitle())) {
            return true;
        }
        //fileIdList
        if(!SetUtils.isEqualSet(new HashSet<>(before.getFileIdList()), new HashSet<>(after.getFileIdList()))) {
            return true;
        }
        //tagList
        if(!SetUtils.isEqualSet(new HashSet<>(before.getTagList()), new HashSet<>(after.getTagList()))) {
            return true;
        }
        //lineList
        List<KnowledgeDocumentLineVo> beforeLineList = before.getLineList();
        List<KnowledgeDocumentLineVo> afterLineList = after.getLineList();
        if(beforeLineList.size() != afterLineList.size()) {
            return true;
        }
        Iterator<KnowledgeDocumentLineVo> beforeLineIterator = beforeLineList.iterator();
        Iterator<KnowledgeDocumentLineVo> afterLineIterator = afterLineList.iterator();
        while(beforeLineIterator.hasNext() && afterLineIterator.hasNext()) {
            KnowledgeDocumentLineVo beforeLine = beforeLineIterator.next();
            KnowledgeDocumentLineVo afterLine = afterLineIterator.next();
            if(!Objects.equals(beforeLine.getHandler(), afterLine.getHandler())) {
                return true;
            }
            String beforeMainBody = KnowledgeDocumentLineHandler.getMainBody(beforeLine);
            String afterMainBody = KnowledgeDocumentLineHandler.getMainBody(afterLine);
            if(!Objects.equals(beforeMainBody, afterMainBody)) {
//                System.out.println(beforeMainBody);
//                System.out.println(afterMainBody);
                return true;
            }
        }
        return false;
    }
}
