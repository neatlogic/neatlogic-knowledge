package codedriver.module.knowledge.api.document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import codedriver.module.knowledge.constvalue.KnowledgeDocumentOperate;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentAuditMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentAuditVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentDraftSubmittedException;
import codedriver.module.knowledge.exception.KnowledgeDocumentCurrentUserNotOwnerException;
import codedriver.module.knowledge.exception.KnowledgeDocumentDraftSubmitFailedExecption;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import codedriver.module.knowledge.exception.KnowledgeDocumentVersionNotFoundException;
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional
public class KnowledgeDocumentDraftSubmitApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    
    @Autowired
    private KnowledgeDocumentAuditMapper knowledgeDocumentAuditMapper;

    @Override
    public String getToken() {
        return "knowledge/document/draft/submit";
    }

    @Override
    public String getName() {
        return "提交审核文档草稿";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
        @Param(name = "knowledgeDocumentVersionId", type = ApiParamType.LONG, isRequired = true, desc = "版本id")
    })
    @Output({
        @Param(name = "isReviewable", type = ApiParamType.ENUM, rule = "0,1", desc = "返回1代表当前用户有审核权限，0代表当前用户没有审核权限")
    })
    @Description(desc = "提交审核文档草稿")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long knowledgeDocumentVersionId = jsonObj.getLong("knowledgeDocumentVersionId");
        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        if(knowledgeDocumentVersionVo == null) {
            throw new KnowledgeDocumentVersionNotFoundException(knowledgeDocumentVersionId);
        }
        if(!knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid(true))) {
            throw new KnowledgeDocumentCurrentUserNotOwnerException();
        }
        knowledgeDocumentMapper.getKnowledgeDocumentLockById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        if(knowledgeDocumentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        }
        knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
//        if(KnowledgeDocumentVersionStatus.EXPIRED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
//            throw new KnowledgeDocumentDraftExpiredCannotSubmitException(knowledgeDocumentVersionId);
//        }else 
        if(!KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentDraftSubmittedException();
        }

        if(knowledgeDocumentMapper.checkIFThereIsSubmittedDraftByKnowDocumentId(knowledgeDocumentVo.getId()) > 0) {
            throw new KnowledgeDocumentDraftSubmitFailedExecption();
        }
        KnowledgeDocumentVersionVo updateStatusVo = new KnowledgeDocumentVersionVo();
        updateStatusVo.setId(knowledgeDocumentVersionId);
        updateStatusVo.setStatus(KnowledgeDocumentVersionStatus.SUBMITTED.getValue());
        knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(updateStatusVo);
        
        int isReviewable = knowledgeDocumentMapper.checkUserIsApprover(UserContext.get().getUserUuid(true), knowledgeDocumentVo.getKnowledgeCircleId());
        JSONObject resultObj = new JSONObject();
        resultObj.put("isReviewable", isReviewable);
        
        KnowledgeDocumentAuditVo knowledgeDocumentAuditVo = new KnowledgeDocumentAuditVo();
        knowledgeDocumentAuditVo.setKnowledgeDocumentId(knowledgeDocumentVo.getId());
        knowledgeDocumentAuditVo.setFcu(UserContext.get().getUserUuid(true));
        knowledgeDocumentAuditVo.setOperate(KnowledgeDocumentOperate.SUBMIT.getValue());
        knowledgeDocumentAuditMapper.insertKnowledgeDocumentAudit(knowledgeDocumentAuditVo);
        return resultObj;
    }

}
