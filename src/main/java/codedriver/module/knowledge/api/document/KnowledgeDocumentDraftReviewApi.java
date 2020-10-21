package codedriver.module.knowledge.api.document;

import java.util.Objects;

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
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentDraftStatusException;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotCurrentVersionException;
import codedriver.module.knowledge.exception.KnowledgeDocumentVersionNotFoundException;
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional
public class KnowledgeDocumentDraftReviewApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

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
        @Param(name = "action", type = ApiParamType.ENUM, rule = "passed,rejected", isRequired = true, desc = "通过，退回"),
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
        knowledgeDocumentMapper.getKnowledgeDocumentLockById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        KnowledgeDocumentVo documentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        if(!Objects.equals(documentVo.getVersion(), knowledgeDocumentVersionVo.getVersion())) {
            throw new KnowledgeDocumentNotCurrentVersionException(knowledgeDocumentVersionVo.getVersion());
        }
        if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentDraftStatusException(knowledgeDocumentVersionId, KnowledgeDocumentVersionStatus.PASSED, "不能再审核");
        }else if(KnowledgeDocumentVersionStatus.REJECTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentDraftStatusException(knowledgeDocumentVersionId, KnowledgeDocumentVersionStatus.REJECTED, "不能再审核");
        }else if(KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentDraftStatusException(knowledgeDocumentVersionId, KnowledgeDocumentVersionStatus.DRAFT, "不能审核");
        }else if(KnowledgeDocumentVersionStatus.EXPIRED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentDraftStatusException(knowledgeDocumentVersionId, KnowledgeDocumentVersionStatus.EXPIRED, "不能审核");
        }
        String action = jsonObj.getString("action");
        KnowledgeDocumentVersionVo updateStatusVo = new KnowledgeDocumentVersionVo();
        updateStatusVo.setId(knowledgeDocumentVersionId);
        updateStatusVo.setStatus(action);
        updateStatusVo.setReviewer(UserContext.get().getUserUuid(true));
        if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(action)) {
            Integer maxVersion = knowledgeDocumentMapper.getKnowledgeDocumentVersionMaxVerionByKnowledgeDocumentId(documentVo.getId());
            if(maxVersion == null) {
                maxVersion = 0;
            }
            updateStatusVo.setVersion(maxVersion + 1);
        }
        knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(updateStatusVo);
        
        if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(action)) {
            knowledgeDocumentMapper.updateKnowledgeDocumentVersionStatusByKnowledgeDocumentIdAndVersionAndStatus(documentVo.getId(), knowledgeDocumentVersionVo.getVersion(), KnowledgeDocumentVersionStatus.DRAFT.getValue(), KnowledgeDocumentVersionStatus.EXPIRED.getValue());
            documentVo.setKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
            documentVo.setVersion(updateStatusVo.getVersion());
            knowledgeDocumentMapper.updateKnowledgeDocumentById(documentVo);
        }
        
        return null;
    }

}