package codedriver.module.knowledge.api.document;

import java.util.List;
import java.util.Objects;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.label.NO_AUTH;
import codedriver.module.knowledge.exception.*;
import org.apache.commons.lang3.StringUtils;
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

@Service
@AuthAction(action = NO_AUTH.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class KnowledgeDocumentDraftReviewApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    
    @Autowired
    private KnowledgeDocumentAuditMapper knowledgeDocumentAuditMapper;

    @Autowired
    private TeamMapper teamMapper;
    
    @Override
    public String getToken() {
        return "knowledge/document/draft/review";
    }

    @Override
    public String getName() {
        return "审核文档草稿";
    }

    @Override
    public String getConfig() {
        return null;
    }
    @Input({
        @Param(name = "knowledgeDocumentVersionId", type = ApiParamType.LONG, isRequired = true, desc = "版本id"),
        @Param(name = "action", type = ApiParamType.ENUM, rule = "pass,reject", isRequired = true, desc = "通过，退回"),
        @Param(name = "content", type = ApiParamType.STRING, desc = "描述")
    })
    @Description(desc = "审核文档草稿")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long knowledgeDocumentVersionId = jsonObj.getLong("knowledgeDocumentVersionId");
        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        if(knowledgeDocumentVersionVo == null) {
            throw new KnowledgeDocumentVersionNotFoundException(knowledgeDocumentVersionId);
        }
        KnowledgeDocumentVo documentVo = knowledgeDocumentMapper.getKnowledgeDocumentLockById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        if(documentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        }
        knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        
        List<String> teamUuidList= teamMapper.getTeamUuidListByUserUuid(UserContext.get().getUserUuid(true));
        if(knowledgeDocumentMapper.checkUserIsApprover(documentVo.getKnowledgeCircleId(), UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList()) == 0) {
            throw new KnowledgeDocumentCurrentUserNotReviewerException();
        }
        if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentDraftReviewedException();
        }else if(KnowledgeDocumentVersionStatus.REJECTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentDraftReviewedException();
        }else if(KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentDraftUnsubmittedCannotBeReviewedException();
        }

        String action = jsonObj.getString("action");
        KnowledgeDocumentVersionVo updateStatusVo = new KnowledgeDocumentVersionVo();
        updateStatusVo.setKnowledgeDocumentTypeUuid(knowledgeDocumentVersionVo.getKnowledgeDocumentTypeUuid());
        updateStatusVo.setId(knowledgeDocumentVersionId);
        updateStatusVo.setReviewer(UserContext.get().getUserUuid(true));
        if(KnowledgeDocumentOperate.PASS.getValue().equals(action)) {
            if(documentVo.getKnowledgeDocumentVersionId() == null){
                if(knowledgeDocumentMapper.checkKnowledgeDocumentTitleIsRepeat(documentVo) > 0){
                    throw new KnowledgeDocumentTitleRepeatException(documentVo.getTitle());
                }
            }

            Integer maxVersion = knowledgeDocumentMapper.getKnowledgeDocumentVersionMaxVerionByKnowledgeDocumentId(documentVo.getId());
            if(maxVersion == null) {
                maxVersion = 0;
            }
            updateStatusVo.setStatus(KnowledgeDocumentVersionStatus.PASSED.getValue());
            updateStatusVo.setVersion(maxVersion + 1);

            documentVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
            documentVo.setVersion(updateStatusVo.getVersion());
            knowledgeDocumentMapper.updateKnowledgeDocumentById(documentVo);
        }else{
            updateStatusVo.setStatus(KnowledgeDocumentVersionStatus.REJECTED.getValue());
        }
        updateStatusVo.setLcu(null);
        knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(updateStatusVo);

        KnowledgeDocumentAuditVo knowledgeDocumentAuditVo = new KnowledgeDocumentAuditVo();
        knowledgeDocumentAuditVo.setKnowledgeDocumentId(documentVo.getId());
        knowledgeDocumentAuditVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
        knowledgeDocumentAuditVo.setFcu(UserContext.get().getUserUuid(true));
        knowledgeDocumentAuditVo.setOperate(action);
        String content = jsonObj.getString("content");
        if(StringUtils.isNotEmpty(content)) {
            JSONObject config = new JSONObject();
            config.put("content", content);
            KnowledgeDocumentAuditConfigVo knowledgeDocumentAuditConfigVo = new KnowledgeDocumentAuditConfigVo(config.toJSONString());
            knowledgeDocumentAuditMapper.insertKnowledgeDocumentAuditConfig(knowledgeDocumentAuditConfigVo);
            knowledgeDocumentAuditVo.setConfigHash(knowledgeDocumentAuditConfigVo.getHash());
        }
        knowledgeDocumentAuditMapper.insertKnowledgeDocumentAudit(knowledgeDocumentAuditVo);
        return null;
    }

}
