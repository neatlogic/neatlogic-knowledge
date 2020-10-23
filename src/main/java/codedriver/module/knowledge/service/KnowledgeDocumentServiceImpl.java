package codedriver.module.knowledge.service;

import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;

public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    @Override
    public int isDeletable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo) {
        if(KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            
        }else if(KnowledgeDocumentVersionStatus.SUBMITTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            
        }else if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            
        }else if(KnowledgeDocumentVersionStatus.REJECTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            
        }else if(KnowledgeDocumentVersionStatus.EXPIRED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            
        }
        return 0;
    }

    @Override
    public int isEditable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo) {
        if(KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            
        }else if(KnowledgeDocumentVersionStatus.SUBMITTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            
        }else if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            
        }else if(KnowledgeDocumentVersionStatus.REJECTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            
        }else if(KnowledgeDocumentVersionStatus.EXPIRED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            
        }
        return 0;
    }

    @Override
    public int isReviewable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo) {
        if(KnowledgeDocumentVersionStatus.DRAFT.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            
        }else if(KnowledgeDocumentVersionStatus.SUBMITTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            
        }else if(KnowledgeDocumentVersionStatus.PASSED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            
        }else if(KnowledgeDocumentVersionStatus.REJECTED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            
        }else if(KnowledgeDocumentVersionStatus.EXPIRED.getValue().equals(knowledgeDocumentVersionVo.getStatus())) {
            
        }
        return 0;
    }

}
