package neatlogic.module.knowledge.api.document;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVo;
import neatlogic.framework.knowledge.exception.KnowledgeDocumentCurrentUserNotReviewerException;
import neatlogic.framework.knowledge.exception.KnowledgeDocumentNotFoundException;
import neatlogic.module.knowledge.service.KnowledgeDocumentService;
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
