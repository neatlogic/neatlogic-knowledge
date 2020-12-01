package codedriver.module.knowledge.dao.mapper;

import java.util.List;

import codedriver.module.knowledge.dto.KnowledgeDocumentAuditConfigVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentAuditVo;

public interface KnowledgeDocumentAuditMapper {

    public List<KnowledgeDocumentAuditVo> getKnowledgeDocumentAuditListByKnowledgeDocumentId(KnowledgeDocumentAuditVo knowledgeDocumentAuditVo);
    
    public KnowledgeDocumentAuditVo getKnowledgeDocumentAuditListByDocumentIdAndVersionIdAndOperate(KnowledgeDocumentAuditVo knowledgeDocumentAuditVo);

    public int getKnowledgeDocumentAuditCountByKnowledgeDocumentId(KnowledgeDocumentAuditVo searchVo);

    public String getKnowledgeDocumentAuditConfigStringByHash(String hash);
    
    public int insertKnowledgeDocumentAudit(KnowledgeDocumentAuditVo knowledgeDocumentAuditVo);
    
    public int insertKnowledgeDocumentAuditConfig(KnowledgeDocumentAuditConfigVo knowledgeDocumentAuditConfigVo);
}
