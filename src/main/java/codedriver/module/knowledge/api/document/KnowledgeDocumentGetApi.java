package codedriver.module.knowledge.api.document;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import codedriver.module.knowledge.service.KnowledgeDocumentService;
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentGetApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;
    
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
        @Param(name = "isReadOnly", type = ApiParamType.ENUM, rule = "0,1", desc = "是否增加浏览量")
    })
    @Output({
        @Param(explode = KnowledgeDocumentVo.class, desc = "文档内容")
    })
    @Description(desc = "查询文档内容")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long knowledgeDocumentId = jsonObj.getLong("knowledgeDocumentId");
        KnowledgeDocumentVo documentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentId);
        if(documentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentId);
        }
        Long knowledgeDocumentVersionId = jsonObj.getLong("knowledgeDocumentVersionId");
        if(knowledgeDocumentVersionId == null) {
            knowledgeDocumentVersionId = documentVo.getKnowledgeDocumentVersionId();
        }
        
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentService.getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        
        knowledgeDocumentVo.setFavorCount(knowledgeDocumentMapper.getDocumentFavorCount(knowledgeDocumentVo.getId()));
        knowledgeDocumentVo.setCollectCount(knowledgeDocumentMapper.getDocumentCollectCount(knowledgeDocumentVo.getId()));
        knowledgeDocumentVo.setPageviews(knowledgeDocumentMapper.getDocumentViewCount(knowledgeDocumentVo.getId()));
        knowledgeDocumentVo.setIsCollect(knowledgeDocumentMapper.checkDocumentHasBeenCollected(knowledgeDocumentVo.getId(), UserContext.get().getUserUuid(true)));
        knowledgeDocumentVo.setIsFavor(knowledgeDocumentMapper.checkDocumentHasBeenFavored(knowledgeDocumentVo.getId(), UserContext.get().getUserUuid(true)));
        
        knowledgeDocumentVo.setIsEditable(knowledgeDocumentService.isEditable(knowledgeDocumentVersionVo));
        knowledgeDocumentVo.setIsDeletable(knowledgeDocumentService.isDeletable(knowledgeDocumentVersionVo));
        knowledgeDocumentVo.setIsReviewable(knowledgeDocumentService.isReviewable(knowledgeDocumentVersionVo));
        
        Integer isReadOnly = jsonObj.getInteger("isReadOnly");
        if(Objects.equals(isReadOnly, 1)) {
            if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(knowledgeDocumentVersionVo.getStatus()) && Objects.equals(knowledgeDocumentVersionId, knowledgeDocumentVo.getKnowledgeDocumentVersionId())) {
                knowledgeDocumentMapper.updateKnowledgeViewCountIncrementOne(knowledgeDocumentVo.getId());
            }
        }
        return knowledgeDocumentVo;
    }

}
