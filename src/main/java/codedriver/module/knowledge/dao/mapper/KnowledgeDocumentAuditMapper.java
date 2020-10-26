package codedriver.module.knowledge.dao.mapper;

import java.util.List;

import codedriver.module.knowledge.dto.KnowledgeDocumentAuditConfigVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentAuditVo;

public interface KnowledgeDocumentAuditMapper {

    public List<KnowledgeDocumentAuditVo> getKnowledgeDocumentAuditByKnowledgeDocumentId(KnowledgeDocumentAuditVo knowledgeDocumentAuditVo);
    
    public int insertKnowledgeDocumentAudit(KnowledgeDocumentAuditVo knowledgeDocumentAuditVo);
    
    public int insertKnowledgeDocumentAuditConfig(KnowledgeDocumentAuditConfigVo knowledgeDocumentAuditConfigVo);
}
