package codedriver.module.knowledge.api.document;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.auth.label.KNOWLEDGE_BASE;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentOperate;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.*;
import codedriver.module.knowledge.service.KnowledgeDocumentService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class KnowledgeDocumentDraftReviewApi extends PrivateApiComponentBase {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;
    
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

        if(knowledgeDocumentService.isReviewer(documentVo.getKnowledgeCircleId()) == 0) {
            throw new KnowledgeDocumentCurrentUserNotReviewerException();
        }
        knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentDraftReviewedException();
        }else if(KnowledgeDocumentVersionStatus.REJECTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentDraftReviewedException();
        }else if(KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentDraftUnsubmittedCannotBeReviewedException();
        }
        KnowledgeDocumentOperate operate = KnowledgeDocumentOperate.PASS;
        String action = jsonObj.getString("action");
        KnowledgeDocumentVersionVo updateStatusVo = new KnowledgeDocumentVersionVo();
        updateStatusVo.setKnowledgeDocumentTypeUuid(knowledgeDocumentVersionVo.getKnowledgeDocumentTypeUuid());
        updateStatusVo.setId(knowledgeDocumentVersionId);
        updateStatusVo.setReviewer(UserContext.get().getUserUuid(true));
        if(knowledgeDocumentVersionVo.getIsDelete() == 1){
            updateStatusVo.setFromVersion(0);
        }
        if(operate.getValue().equals(action)) {
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
            operate = KnowledgeDocumentOperate.REJECT;
            updateStatusVo.setStatus(KnowledgeDocumentVersionStatus.REJECTED.getValue());
        }
        knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(updateStatusVo);
        JSONObject config = null;
        String content = jsonObj.getString("content");
        if(StringUtils.isNotEmpty(content)) {
            config = new JSONObject();
            config.put("content", content);
        }
        knowledgeDocumentService.audit(documentVo.getId(), knowledgeDocumentVersionId, operate, config);
        return null;
    }

}
