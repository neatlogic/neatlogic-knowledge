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
import neatlogic.framework.knowledge.event.KnowledgeDocumentChangedEvent;
import neatlogic.framework.knowledge.exception.KnowledgeDocumentCurrentUserNotReviewerException;
import neatlogic.framework.knowledge.exception.KnowledgeDocumentNotFoundException;
import neatlogic.module.knowledge.service.KnowledgeDocumentService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class KnowledgeDocumentDeleteApi extends PrivateApiComponentBase {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;
    @Resource
    private ApplicationEventPublisher eventPublisher;
    @Override
    public String getToken() {
        return "knowledge/document/delete";
    }

    @Override
    public String getName() {
        return "nmkad.knowledgedocumentdeleteapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({
        @Param(name = "knowledgeDocumentId", type = ApiParamType.LONG, isRequired = true, desc = "nmkad.knowledgedocumentdeleteapi.documentid")
    })
    @Description(desc = "nmkad.knowledgedocumentdeleteapi.getname")
    /** 删除整篇文档及关联信息后，为首次删除发布事件。 */
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long knowledgeDocumentId = jsonObj.getLong("knowledgeDocumentId");
        // 与审批及回退共用文档锁，串行判断删除状态。
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentLockById(knowledgeDocumentId);
        if(knowledgeDocumentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentId);
        }
        if(knowledgeDocumentService.isReviewer(knowledgeDocumentVo.getKnowledgeCircleId()) == 0) {
            throw new KnowledgeDocumentCurrentUserNotReviewerException();
        }
        boolean documentChanged = !Objects.equals(knowledgeDocumentVo.getIsDelete(), 1);
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
        if (documentChanged) {
            eventPublisher.publishEvent(new KnowledgeDocumentChangedEvent(
                    KnowledgeDocumentChangedEvent.Type.DOCUMENT_DELETED, knowledgeDocumentId,
                    knowledgeDocumentVo.getKnowledgeDocumentVersionId()));
        }
        return null;
    }

}
