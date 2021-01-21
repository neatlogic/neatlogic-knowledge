package codedriver.module.knowledge.api.document;

import java.util.List;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.label.NO_AUTH;
import codedriver.module.knowledge.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
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

@Service
@AuthAction(action = NO_AUTH.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class KnowledgeDocumentDraftSubmitApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    
    @Autowired
    private KnowledgeDocumentAuditMapper knowledgeDocumentAuditMapper;

    @Autowired
    private TeamMapper teamMapper;
    
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
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentLockById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        if(knowledgeDocumentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        }
        if(knowledgeDocumentVo.getKnowledgeDocumentVersionId() == null){
            if(knowledgeDocumentMapper.checkKnowledgeDocumentTitleIsRepeat(knowledgeDocumentVo) > 0){
                throw new KnowledgeDocumentTitleRepeatException(knowledgeDocumentVo.getTitle());
            }
        }
        knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);

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
        
        List<String> teamUuidList= teamMapper.getTeamUuidListByUserUuid(UserContext.get().getUserUuid(true));
        int isReviewable = knowledgeDocumentMapper.checkUserIsApprover(knowledgeDocumentVo.getKnowledgeCircleId(), UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList());
        JSONObject resultObj = new JSONObject();
        resultObj.put("isReviewable", isReviewable);
        
        KnowledgeDocumentAuditVo knowledgeDocumentAuditVo = new KnowledgeDocumentAuditVo();
        knowledgeDocumentAuditVo.setKnowledgeDocumentId(knowledgeDocumentVo.getId());
        knowledgeDocumentAuditVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
        knowledgeDocumentAuditVo.setFcu(UserContext.get().getUserUuid(true));
        knowledgeDocumentAuditVo.setOperate(KnowledgeDocumentOperate.SUBMIT.getValue());
        knowledgeDocumentAuditMapper.insertKnowledgeDocumentAudit(knowledgeDocumentAuditVo);
        return resultObj;
    }

}
