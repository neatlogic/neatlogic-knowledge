package neatlogic.module.knowledge.approve.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.knowledge.constvalue.KnowledgeDocumentOperate;
import neatlogic.framework.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVersionVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVo;
import neatlogic.framework.knowledge.exception.*;
import neatlogic.framework.process.approve.core.ApproveHandlerBase;
import neatlogic.framework.process.approve.dto.ApproveEntityVo;
import neatlogic.framework.process.constvalue.ProcessTaskFinalStatus;
import neatlogic.module.knowledge.approve.constvalue.KnowledgeApproveHandlerType;
import neatlogic.module.knowledge.service.KnowledgeDocumentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class KnowledgeApproveHandler extends ApproveHandlerBase {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Override
    public String getHandler() {
        return KnowledgeApproveHandlerType.KNOWLEDGE_DOCUMENT.getValue();
    }

    @Override
    protected ApproveEntityVo myStartProcess(Long id) throws PermissionDeniedException {
        JSONObject versionCompareResultObj = knowledgeDocumentService.getKnowledgeDocumentVersionCompare(id, null);
        KnowledgeDocumentVo newDocumentVo = versionCompareResultObj.getObject("newDocumentVo", KnowledgeDocumentVo.class);
        ApproveEntityVo approveEntity = new ApproveEntityVo();
        approveEntity.setId(id);
        approveEntity.setType(KnowledgeApproveHandlerType.KNOWLEDGE_DOCUMENT.getValue());
        approveEntity.setSource("knowledgeDocument");
        approveEntity.setTitle(newDocumentVo.getTitle());
        approveEntity.setEntity(versionCompareResultObj);
        return approveEntity;
    }

    @Override
    public int callback(Long processTaskId, ProcessTaskFinalStatus finalStatus, Long id, String content) {
        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(id);
        if(knowledgeDocumentVersionVo == null) {
            throw new KnowledgeDocumentVersionNotFoundException(id);
        }
        KnowledgeDocumentVo documentVo = knowledgeDocumentMapper.getKnowledgeDocumentLockById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        if(documentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        }

        if(knowledgeDocumentService.isReviewer(documentVo.getKnowledgeCircleId()) == 0) {
            throw new KnowledgeDocumentCurrentUserNotReviewerException();
        }
        knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(id);
        if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentDraftReviewedException();
        }else if(KnowledgeDocumentVersionStatus.REJECTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentDraftReviewedException();
        }else if(KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            throw new KnowledgeDocumentDraftUnsubmittedCannotBeReviewedException();
        }
        KnowledgeDocumentOperate operate = KnowledgeDocumentOperate.PASS;
        KnowledgeDocumentVersionVo updateStatusVo = new KnowledgeDocumentVersionVo();
        updateStatusVo.setKnowledgeDocumentTypeUuid(knowledgeDocumentVersionVo.getKnowledgeDocumentTypeUuid());
        updateStatusVo.setId(id);
        updateStatusVo.setReviewer(UserContext.get().getUserUuid(true));
        if(knowledgeDocumentVersionVo.getIsDelete() == 1){
            updateStatusVo.setFromVersion(0);
        }
        if (ProcessTaskFinalStatus.FAILED == finalStatus) {
            operate = KnowledgeDocumentOperate.REJECT;
        }
        if(operate == KnowledgeDocumentOperate.PASS) {
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

            documentVo.setKnowledgeDocumentVersionId(id);
            documentVo.setVersion(updateStatusVo.getVersion());
            knowledgeDocumentMapper.updateKnowledgeDocumentById(documentVo);
        }else{
            operate = KnowledgeDocumentOperate.REJECT;
            updateStatusVo.setStatus(KnowledgeDocumentVersionStatus.REJECTED.getValue());
        }
        knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(updateStatusVo);
        JSONObject config = null;
        if(StringUtils.isNotEmpty(content)) {
            config = new JSONObject();
            config.put("content", content);
        }
        knowledgeDocumentService.audit(documentVo.getId(), id, operate, config);
        return 1;
    }
}
