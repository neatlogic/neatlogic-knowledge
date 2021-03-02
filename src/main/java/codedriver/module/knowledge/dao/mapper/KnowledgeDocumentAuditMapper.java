package codedriver.module.knowledge.dao.mapper;

import codedriver.module.knowledge.dto.KnowledgeDocumentAuditConfigVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentAuditVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KnowledgeDocumentAuditMapper {

    public List<KnowledgeDocumentAuditVo> getKnowledgeDocumentAuditListByKnowledgeDocumentId(KnowledgeDocumentAuditVo knowledgeDocumentAuditVo);
    
    public KnowledgeDocumentAuditVo getKnowledgeDocumentAuditListByDocumentIdAndVersionIdAndOperate(KnowledgeDocumentAuditVo knowledgeDocumentAuditVo);

    public List<KnowledgeDocumentAuditVo> getKnowledgeDocumentAuditListByDocumentVersionIdListAndOperate(@Param("versionIdList")List<Long> versionIdList,@Param("operate") String operate);

    public int getKnowledgeDocumentAuditCountByKnowledgeDocumentId(KnowledgeDocumentAuditVo searchVo);

    public String getKnowledgeDocumentAuditConfigStringByHash(String hash);
    
    public int insertKnowledgeDocumentAudit(KnowledgeDocumentAuditVo knowledgeDocumentAuditVo);
    
    public int insertKnowledgeDocumentAuditConfig(KnowledgeDocumentAuditConfigVo knowledgeDocumentAuditConfigVo);
}
