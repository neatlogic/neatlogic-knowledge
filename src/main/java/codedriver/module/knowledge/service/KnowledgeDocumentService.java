package codedriver.module.knowledge.service;

import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;

public interface KnowledgeDocumentService {

    public int isDeletable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);
    
    public int isEditable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);
    
    public int isReviewable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);
}
