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
import codedriver.framework.knowledge.constvalue.KnowledgeDocumentOperate;
import codedriver.framework.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.framework.knowledge.dto.KnowledgeDocumentFileVo;
import codedriver.framework.knowledge.dto.KnowledgeDocumentTagVo;
import codedriver.framework.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.framework.knowledge.dto.KnowledgeDocumentVo;
import codedriver.framework.knowledge.exception.KnowledgeDocumentCurrentUserNotOwnerException;
import codedriver.framework.knowledge.exception.KnowledgeDocumentCurrentUserNotReviewerException;
import codedriver.framework.knowledge.exception.KnowledgeDocumentCurrentVersionCannotBeDeletedException;
import codedriver.framework.knowledge.exception.KnowledgeDocumentDraftSubmittedCannotBeDeletedException;
import codedriver.module.knowledge.service.KnowledgeDocumentService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class KnowledgeDocumentVersionDeleteApi extends PrivateApiComponentBase {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;
    
    @Override
    public String getToken() {
        return "knowledge/document/version/delete";
    }

    @Override
    public String getName() {
        return "删除文档版本";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({
        @Param(name = "knowledgeDocumentVersionId", type = ApiParamType.LONG, isRequired = true, desc = "版本id")
    })
    @Description(desc = "删除文档版本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long knowledgeDocumentVersionId = jsonObj.getLong("knowledgeDocumentVersionId");
        KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        if(knowledgeDocumentVersionVo == null) {
            return null;
        }
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentLockById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
        if(knowledgeDocumentVo == null){
            knowledgeDocumentMapper.deleteKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
            knowledgeDocumentMapper.deleteKnowledgeDocumentLineByKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
            knowledgeDocumentMapper.deleteKnowledgeDocumentFileByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentFileVo(knowledgeDocumentVersionVo.getKnowledgeDocumentId(), knowledgeDocumentVersionId));
            knowledgeDocumentMapper.deleteKnowledgeDocumentTagByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentTagVo(knowledgeDocumentVersionVo.getKnowledgeDocumentId(), knowledgeDocumentVersionId));
            return null;
        }
        if(Objects.equals(knowledgeDocumentVo.getKnowledgeDocumentVersionId(), knowledgeDocumentVersionId)) {
            throw new KnowledgeDocumentCurrentVersionCannotBeDeletedException();
        }
        knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
        if(knowledgeDocumentVersionVo == null) {
            return null;
        }
        if(knowledgeDocumentVersionVo.getStatus().equals(KnowledgeDocumentVersionStatus.PASSED.getValue())) {
            if(knowledgeDocumentService.isReviewer(knowledgeDocumentVo.getKnowledgeCircleId()) == 0) {
                throw new KnowledgeDocumentCurrentUserNotReviewerException();
            }
            knowledgeDocumentMapper.updateKnowledgeDocumentVersionToDeleteById(knowledgeDocumentVersionVo.getId());
            JSONObject config = new JSONObject();
            config.put("version", knowledgeDocumentVersionVo.getVersion());
            knowledgeDocumentService.audit(knowledgeDocumentVersionVo.getKnowledgeDocumentId(), knowledgeDocumentVersionId, KnowledgeDocumentOperate.DELETEVERSION, config);
        }else if(knowledgeDocumentVersionVo.getStatus().equals(KnowledgeDocumentVersionStatus.SUBMITTED.getValue())) {
            throw new KnowledgeDocumentDraftSubmittedCannotBeDeletedException();
        }else {
            if(!knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid(true))) {
                throw new KnowledgeDocumentCurrentUserNotOwnerException();
            }
            knowledgeDocumentMapper.deleteKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
            knowledgeDocumentMapper.deleteKnowledgeDocumentLineByKnowledgeDocumentVersionId(knowledgeDocumentVersionId);
            knowledgeDocumentMapper.deleteKnowledgeDocumentFileByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentFileVo(knowledgeDocumentVersionVo.getKnowledgeDocumentId(), knowledgeDocumentVersionId));
            knowledgeDocumentMapper.deleteKnowledgeDocumentTagByKnowledgeDocumentIdAndVersionId(new KnowledgeDocumentTagVo(knowledgeDocumentVersionVo.getKnowledgeDocumentId(), knowledgeDocumentVersionId));
        }
        return null;
    }

}
