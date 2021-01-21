package codedriver.module.knowledge.service;

import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;

public interface KnowledgeDocumentService {

    public int isDeletable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);
    
    public int isEditable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);
    
    public int isReviewer(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);
    
    public KnowledgeDocumentVo getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(Long knowledgeDocumentVersionId) throws PermissionDeniedException;
}
