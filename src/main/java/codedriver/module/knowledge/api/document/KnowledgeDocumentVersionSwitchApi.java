package codedriver.module.knowledge.api.document;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.label.NO_AUTH;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentOperate;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentAuditMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentAuditConfigVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentAuditVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentCurrentUserNotReviewerException;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotHistoricalVersionException;
import codedriver.module.knowledge.exception.KnowledgeDocumentVersionNotFoundException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@AuthAction(action = NO_AUTH.class)
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class KnowledgeDocumentVersionSwitchApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    
    @Autowired
    private KnowledgeDocumentAuditMapper knowledgeDocumentAuditMapper;

    @Autowired
    private TeamMapper teamMapper;
    
    @Override
    public String getToken() {
        return "knowledge/document/version/switch";
    }

    @Override
    public String getName() {
        return "切换文档版本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
        @Param(name = "knowledgeDocumentVersionId", type = ApiParamType.LONG, isRequired = true, desc = "版本id")
    })
    @Description(desc = "切换文档版本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long knowledgeDocumentVersionId = jsonObj.getLong("knowledgeDocumentVersionId");
        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        if(knowledgeDocumentVersionVo == null) {
            throw new KnowledgeDocumentVersionNotFoundException(knowledgeDocumentVersionId);
        }
        knowledgeDocumentMapper.getKnowledgeDocumentLockById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        if(KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentNotHistoricalVersionException(knowledgeDocumentVersionId);
        }else if(KnowledgeDocumentVersionStatus.SUBMITTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentNotHistoricalVersionException(knowledgeDocumentVersionId);
        }
//        else if(KnowledgeDocumentVersionStatus.EXPIRED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
//            throw new KnowledgeDocumentNotHistoricalVersionException(knowledgeDocumentVersionId);
//        }
        else if(KnowledgeDocumentVersionStatus.REJECTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentNotHistoricalVersionException(knowledgeDocumentVersionId);
        }
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        if(knowledgeDocumentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        }
//        if(knowledgeDocumentMapper.checkIFThereIsSubmittedDraftByKnowDocumentIdAndFromVersion(knowledgeDocumentVo.getId(), knowledgeDocumentVo.getVersion()) > 0) {
//            throw new KnowledgeDocumentVersionSwitchFailedExecption();
//        }
        int oldVersion = knowledgeDocumentVo.getVersion();
        List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(UserContext.get().getUserUuid(true));
        if(knowledgeDocumentMapper.checkUserIsApprover(knowledgeDocumentVo.getKnowledgeCircleId(), UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList()) == 0) {
            throw new KnowledgeDocumentCurrentUserNotReviewerException();
        }

        knowledgeDocumentVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
        knowledgeDocumentVo.setVersion(knowledgeDocumentVersionVo.getVersion());
        knowledgeDocumentVo.setKnowledgeDocumentTypeUuid(knowledgeDocumentVersionVo.getKnowledgeDocumentTypeUuid());
        knowledgeDocumentMapper.updateKnowledgeDocumentById(knowledgeDocumentVo);
        
        KnowledgeDocumentAuditVo knowledgeDocumentAuditVo = new KnowledgeDocumentAuditVo();
        knowledgeDocumentAuditVo.setKnowledgeDocumentId(knowledgeDocumentVo.getId());
        knowledgeDocumentAuditVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
        knowledgeDocumentAuditVo.setFcu(UserContext.get().getUserUuid(true));
        knowledgeDocumentAuditVo.setOperate(KnowledgeDocumentOperate.SWITCHVERSION.getValue());
        JSONObject config = new JSONObject();
        config.put("oldVersion", oldVersion);
        config.put("newVersion", knowledgeDocumentVersionVo.getVersion());
        KnowledgeDocumentAuditConfigVo knowledgeDocumentAuditConfigVo = new KnowledgeDocumentAuditConfigVo(config.toJSONString());
        knowledgeDocumentAuditMapper.insertKnowledgeDocumentAuditConfig(knowledgeDocumentAuditConfigVo);
        knowledgeDocumentAuditVo.setConfigHash(knowledgeDocumentAuditConfigVo.getHash());
        knowledgeDocumentAuditMapper.insertKnowledgeDocumentAudit(knowledgeDocumentAuditVo);
        return null;
    }

}
