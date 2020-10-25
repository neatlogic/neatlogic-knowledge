package codedriver.module.knowledge.api.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.UuidUtil;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeTagMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentFileVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineConfigVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineContentVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentTagVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.dto.KnowledgeTagVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentDraftExpiredCannotBeModifiedException;
import codedriver.module.knowledge.exception.KnowledgeDocumentDraftPublishedCannotBeModifiedException;
import codedriver.module.knowledge.exception.KnowledgeDocumentDraftSubmittedCannotBeModifiedException;
import codedriver.module.knowledge.exception.KnowledgeDocumentHasBeenDeletedException;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotCurrentVersionException;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import codedriver.module.knowledge.exception.KnowledgeDocumentVersionNotFoundException;
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional
public class KnowledgeDocumentDraftSaveApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Autowired
    private KnowledgeTagMapper knowledgeTagMapper;

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
        @Param(name = "knowledgeCircleId", type = ApiParamType.LONG, isRequired = true, desc = "知识圈id"),
        @Param(name = "title", type = ApiParamType.STRING, isRequired = true, minLength = 1, desc = "标题"),
        @Param(name = "lineList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "行数据列表"),
        @Param(name = "fileIdList", type = ApiParamType.JSONARRAY, desc = "附件id列表"),
        @Param(name = "tagList", type = ApiParamType.JSONARRAY, desc = "标签列表")
    })
    @Output({
        @Param(name = "knowledgeDocumentId", type = ApiParamType.LONG, desc = "文档id"),
        @Param(name = "knowledgeDocumentVersionId", type = ApiParamType.LONG, desc = "版本id")
    })
    @Description(desc = "保存文档草稿")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long knowledgeDocumentVersionId = jsonObj.getLong("knowledgeDocumentVersionId");
        KnowledgeDocumentVo documentVo = JSON.toJavaObject(jsonObj, KnowledgeDocumentVo.class);
        documentVo.setId(null);
        Long documentId = null;
        Long drafrVersionId = null;
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
                /** 如果入参版本id是文档当前版本id，说明该操作是当前版本上修改首次存草稿 **/
                KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = new KnowledgeDocumentVersionVo();
                knowledgeDocumentVersionVo.setTitle(documentVo.getTitle());
                knowledgeDocumentVersionVo.setKnowledgeDocumentTypeUuid(documentVo.getKnowledgeDocumentTypeUuid());
                knowledgeDocumentVersionVo.setKnowledgeDocumentId(documentId);
                knowledgeDocumentVersionVo.setVersion(oldKnowledgeDocumentVersionVo.getVersion());
                knowledgeDocumentVersionVo.setLcu(UserContext.get().getUserUuid(true));
                knowledgeDocumentVersionVo.setStatus(KnowledgeDocumentVersionStatus.DRAFT.getValue());
                knowledgeDocumentMapper.insertKnowledgeDocumentVersion(knowledgeDocumentVersionVo);
                drafrVersionId = knowledgeDocumentVersionVo.getId();
            }else {
                if(!Objects.equals(oldDocumentVo.getVersion(), oldKnowledgeDocumentVersionVo.getVersion())) {
                    throw new KnowledgeDocumentNotCurrentVersionException(oldKnowledgeDocumentVersionVo.getVersion());
                }
                /** 如果入参版本id不是文档当前版本id，说明该操作是在已有草稿上再次保存 **/
                if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(oldKnowledgeDocumentVersionVo.getStatus())) {
                    throw new KnowledgeDocumentDraftPublishedCannotBeModifiedException();
                }else if(KnowledgeDocumentVersionStatus.SUBMITTED.getValue().equals(oldKnowledgeDocumentVersionVo.getStatus())) {
                    throw new KnowledgeDocumentDraftSubmittedCannotBeModifiedException();
                }else if(KnowledgeDocumentVersionStatus.EXPIRED.getValue().equals(oldKnowledgeDocumentVersionVo.getStatus())) {
                    throw new KnowledgeDocumentDraftExpiredCannotBeModifiedException();
                }
                drafrVersionId = knowledgeDocumentVersionId;
                /** 覆盖旧草稿时，更新标题、修改用户、修改时间，删除行数据、附件、标签数据，后面再重新插入 **/
                KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = new KnowledgeDocumentVersionVo();
                knowledgeDocumentVersionVo.setId(drafrVersionId);
                knowledgeDocumentVersionVo.setKnowledgeDocumentTypeUuid(documentVo.getKnowledgeDocumentTypeUuid());
                knowledgeDocumentVersionVo.setTitle(documentVo.getTitle());
                knowledgeDocumentVersionVo.setStatus(KnowledgeDocumentVersionStatus.DRAFT.getValue());
                knowledgeDocumentVersionVo.setLcu(UserContext.get().getUserUuid(true));
                knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(knowledgeDocumentVersionVo);
                knowledgeDocumentMapper.deleteKnowledgeDocumentLineByKnowledgeDocumentVersionId(drafrVersionId);
                knowledgeDocumentMapper.deleteKnowledgeDocumentFileByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentFileVo(documentId, drafrVersionId));
                knowledgeDocumentMapper.deleteKnowledgeDocumentTagByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentTagVo(documentId, drafrVersionId));
            }
        }else {
            /** 没有版本id，则是首次创建文档 **/
            KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = new KnowledgeDocumentVersionVo();
            documentVo.setFcu(UserContext.get().getUserUuid(true));
            documentVo.setVersion(0);
            knowledgeDocumentMapper.insertKnowledgeDocument(documentVo);
            knowledgeDocumentMapper.insertKnowledgeDocumentViewCount(documentVo.getId(), 0);
            knowledgeDocumentVersionVo.setTitle(documentVo.getTitle());
            knowledgeDocumentVersionVo.setKnowledgeDocumentTypeUuid(documentVo.getKnowledgeDocumentTypeUuid());
            knowledgeDocumentVersionVo.setKnowledgeDocumentId(documentVo.getId());
            knowledgeDocumentVersionVo.setVersion(0);
            knowledgeDocumentVersionVo.setLcu(UserContext.get().getUserUuid(true));
            knowledgeDocumentVersionVo.setStatus(KnowledgeDocumentVersionStatus.DRAFT.getValue());
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
        JSONObject resultObj = new JSONObject();
        resultObj.put("knowledgeDocumentId", documentId);
        resultObj.put("knowledgeDocumentVersionId", drafrVersionId);
        return resultObj;
    }

}
