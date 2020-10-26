package codedriver.module.knowledge.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;

@Service
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Override
    public int isDeletable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo) {
        if(KnowledgeDocumentVersionStatus.SUBMITTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            return 0;
        }else if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            if(knowledgeDocumentVersionVo.getReviewer().equals(UserContext.get().getUserUuid())) {
                if(knowledgeDocumentMapper.checkIfTheVersionIsTheCurrentVersion(knowledgeDocumentVersionVo) == 0) {
                    return 1;
                }
            }
        }else {
            if(knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid())) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public int isEditable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo) {
        if(KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            if(knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid())) {
                return 1;
            }
        }else if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            return knowledgeDocumentMapper.checkIfTheVersionIsTheCurrentVersion(knowledgeDocumentVersionVo);
        }else if(KnowledgeDocumentVersionStatus.REJECTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            if(knowledgeDocumentVersionVo.getLcu().equals(UserContext.get().getUserUuid())) {
                return knowledgeDocumentMapper.checkIfTheVersionIsTheCurrentVersion(knowledgeDocumentVersionVo);
            }
        }
        return 0;
    }

    @Override
    public int isReviewable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo) {
        if(KnowledgeDocumentVersionStatus.SUBMITTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentVersionVo.getKnowledgeDocumentId());
            if(knowledgeDocumentMapper.checkUserIsApprover(UserContext.get().getUserUuid(true), knowledgeDocumentVo.getKnowledgeCircleId()) > 0) {
                return 1;
            }
        }
        return 0;
    }

}
