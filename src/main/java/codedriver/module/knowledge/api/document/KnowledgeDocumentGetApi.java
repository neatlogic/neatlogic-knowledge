package codedriver.module.knowledge.api.document;

import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dao.mapper.TagMapper;
import codedriver.framework.dto.TagVo;
import codedriver.framework.file.dao.mapper.FileMapper;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dao.mapper.KnowledgeCircleMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeCircleUserVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentFileVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentTagVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import codedriver.module.knowledge.exception.KnowledgeDocumentVersionNotFoundException;
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentGetApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Autowired
    private KnowledgeCircleMapper knowledgeCircleMapper;
    @Autowired
    private FileMapper fileMapper;
    @Autowired
    private TagMapper tagMapper;

    @Override
    public String getToken() {
        return "knowledge/document/get";
    }

    @Override
    public String getName() {
        return "查询文档内容";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({
        @Param(name = "knowledgeDocumentId", type = ApiParamType.LONG, isRequired = true, desc = "文档id"),
        @Param(name = "knowledgeDocumentVersionId", type = ApiParamType.LONG, desc = "版本id"),
    })
    @Output({
        @Param(explode = KnowledgeDocumentVo.class, desc = "文档内容")
    })
    @Description(desc = "查询文档内容")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long knowledgeDocumentId = jsonObj.getLong("knowledgeDocumentId");
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentId);
        if(knowledgeDocumentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentId);
        }
        Long knowledgeDocumentVersionId = jsonObj.getLong("knowledgeDocumentVersionId");
        if(knowledgeDocumentVersionId == null) {
            knowledgeDocumentVersionId = knowledgeDocumentVo.getKnowledgeDocumentVersionId();
        }
        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        if(knowledgeDocumentVersionVo == null) {
            throw new KnowledgeDocumentVersionNotFoundException(knowledgeDocumentVersionId);
        }
        List<KnowledgeDocumentLineVo> lineList = knowledgeDocumentMapper.getKnowledgeDocumentLineListByKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
        knowledgeDocumentVo.setLineList(lineList);
        List<Long> fileIdList = knowledgeDocumentMapper.getKnowledgeDocumentFileIdListByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentFileVo(knowledgeDocumentId, knowledgeDocumentVersionId));
        if(CollectionUtils.isNotEmpty(fileIdList)) {
            List<FileVo> fileList = fileMapper.getFileListByIdList(fileIdList);
            knowledgeDocumentVo.setFileIdList(fileIdList);
            knowledgeDocumentVo.setFileList(fileList);
        }
        List<Long> tagIdList = knowledgeDocumentMapper.getKnowledgeDocumentTagIdListByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentTagVo(knowledgeDocumentId, knowledgeDocumentVersionId));
        if(CollectionUtils.isNotEmpty(tagIdList)) {
            List<TagVo> tagList = tagMapper.getTagListByIdList(tagIdList);
            knowledgeDocumentVo.setTagIdList(tagIdList);
            knowledgeDocumentVo.setTagList(tagList);
        }
        knowledgeDocumentVo.setIsEditable(0);
        knowledgeDocumentVo.setIsDeletable(0);
        knowledgeDocumentVo.setIsReviewable(0);
        int isReviewable = 0;
        List<KnowledgeCircleUserVo> knowledgeCircleUserList = knowledgeCircleMapper.getKnowledgeCircleUserListByIdAndAuthType(knowledgeDocumentVo.getKnowledgeCircleId(), KnowledgeCircleUserVo.AuthType.APPROVER.getValue());
        for(KnowledgeCircleUserVo knowledgeCircleUserVo : knowledgeCircleUserList) {
            if(GroupSearch.USER.getValue().equals(knowledgeCircleUserVo.getType())) {
               if(UserContext.get().getUserUuid(true).equals(knowledgeCircleUserVo.getUuid())) {
                   isReviewable = 1;
               }
            }
        }
        if(KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            if(UserContext.get().getUserUuid(true).equals(knowledgeDocumentVersionVo.getLcu())) {
                knowledgeDocumentVo.setIsEditable(1);
                knowledgeDocumentVo.setIsDeletable(1);
            }
            knowledgeDocumentVo.setIsReviewable(isReviewable);
        }else if(KnowledgeDocumentVersionStatus.SUBMITED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            knowledgeDocumentVo.setIsReviewable(isReviewable);
        }else if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            knowledgeDocumentVo.setIsEditable(1);
        }else if(KnowledgeDocumentVersionStatus.REJECTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            if(Objects.equals(knowledgeDocumentVo.getVersion(), knowledgeDocumentVersionVo.getVersion())) {
                knowledgeDocumentVo.setIsEditable(1);
                knowledgeDocumentVo.setIsDeletable(1);
                knowledgeDocumentVo.setIsReviewable(isReviewable);
            }
        }
        
        return knowledgeDocumentVo;
    }

}