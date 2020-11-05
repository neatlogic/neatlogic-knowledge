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
import codedriver.module.knowledge.dto.KnowledgeDocumentFileVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentTagVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentCurrentUserNotOwnerException;
import codedriver.module.knowledge.exception.KnowledgeDocumentCurrentUserNotReviewerException;
import codedriver.module.knowledge.exception.KnowledgeDocumentCurrentVersionCannotBeDeletedException;
import codedriver.module.knowledge.exception.KnowledgeDocumentDraftSubmittedCannotBeDeletedException;
@Service
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class KnowledgeDocumentVersionDeleteApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

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
        if(knowledgeDocumentVersionVo != null) {
            knowledgeDocumentMapper.getKnowledgeDocumentLockById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
            KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
            if(knowledgeDocumentVo != null) {
                if(Objects.equals(knowledgeDocumentVo.getKnowledgeDocumentVersionId(), knowledgeDocumentVersionId)) {
                    throw new KnowledgeDocumentCurrentVersionCannotBeDeletedException();
                }
                knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVersionId);
                if(knowledgeDocumentVersionVo.getStatus().equals(KnowledgeDocumentVersionStatus.PASSED.getValue())) {
                    if(knowledgeDocumentMapper.checkUserIsApprover(UserContext.get().getUserUuid(true), knowledgeDocumentVo.getKnowledgeCircleId()) == 0) {
                        throw new KnowledgeDocumentCurrentUserNotReviewerException();
                    }
                    knowledgeDocumentMapper.updateKnowledgeDocumentVersionToDeleteById(knowledgeDocumentVersionVo.getId());
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
            }
        }
        return null;
    }

}
