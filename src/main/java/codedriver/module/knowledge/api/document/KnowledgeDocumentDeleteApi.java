package codedriver.module.knowledge.api.document;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.auth.label.KNOWLEDGE_BASE;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentCurrentUserNotReviewerException;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import codedriver.module.knowledge.service.KnowledgeDocumentService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class KnowledgeDocumentDeleteApi extends PrivateApiComponentBase {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;
    @Override
    public String getToken() {
        return "knowledge/document/delete";
    }

    @Override
    public String getName() {
        return "删除文档";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({
        @Param(name = "knowledgeDocumentId", type = ApiParamType.LONG, isRequired = true, desc = "文档id")
    })
    @Description(desc = "删除文档")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long knowledgeDocumentId = jsonObj.getLong("knowledgeDocumentId");
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentId);
        if(knowledgeDocumentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentId);
        }
        if(knowledgeDocumentService.isReviewer(knowledgeDocumentVo.getKnowledgeCircleId()) == 0) {
            throw new KnowledgeDocumentCurrentUserNotReviewerException();
        }
        knowledgeDocumentMapper.updateKnowledgeDocumentToDeleteById(knowledgeDocumentId);
        knowledgeDocumentMapper.deleteKnowledgeDocumentInvokeByKnowledgeDocumentId(knowledgeDocumentId);
        knowledgeDocumentMapper.deleteKnowledgeDocumentCollectByDocumentId(knowledgeDocumentId);
        knowledgeDocumentMapper.deleteKnowledgeDocumentFavorByDocumentId(knowledgeDocumentId);
        knowledgeDocumentMapper.resetKnowledgeViewCountByDocumentId(knowledgeDocumentId);

        List<Long> knowledgeDocumentVersionIdList =  knowledgeDocumentMapper.getKnowledgeDocumentHistorialVersionIdListByKnowledgeDocumentId(knowledgeDocumentId);
        if(CollectionUtils.isNotEmpty(knowledgeDocumentVersionIdList)){
            knowledgeDocumentMapper.updateKnowledgeDocumentVersionToDeleteByKnowledgeDocumentId(knowledgeDocumentId);
            knowledgeDocumentMapper.deleteKnowledgeDocumentAuditByKnowledgeDocumentVersionIdList(knowledgeDocumentVersionIdList);
        }
        //** 删除es对应知识 **//
        //ElasticSearchHandlerFactory.getHandler(ESHandler.KNOWLEDGE.getValue()).delete(knowledgeDocumentId.toString());
        return null;
    }

}
