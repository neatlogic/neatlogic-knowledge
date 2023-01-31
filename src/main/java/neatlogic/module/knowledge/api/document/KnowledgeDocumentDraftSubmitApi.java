package neatlogic.module.knowledge.api.document;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import neatlogic.framework.knowledge.constvalue.KnowledgeDocumentOperate;
import neatlogic.framework.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVersionVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVo;
import neatlogic.framework.knowledge.exception.*;
import neatlogic.module.knowledge.service.KnowledgeDocumentService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class KnowledgeDocumentDraftSubmitApi extends PrivateApiComponentBase {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;
    
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
        if(knowledgeDocumentVersionVo.getIsDelete() == 1){
            updateStatusVo.setFromVersion(0);
        }
        knowledgeDocumentMapper.updateKnowledgeDocumentVersionById(updateStatusVo);

        int isReviewable = knowledgeDocumentService.isReviewer(knowledgeDocumentVo.getKnowledgeCircleId());
        JSONObject resultObj = new JSONObject();
        resultObj.put("isReviewable", isReviewable);

        knowledgeDocumentService.audit(knowledgeDocumentVo.getId(), knowledgeDocumentVersionId, KnowledgeDocumentOperate.SUBMIT, null);
        return resultObj;
    }

}
