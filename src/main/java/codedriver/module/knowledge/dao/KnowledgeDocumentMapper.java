package codedriver.module.knowledge.dao;

import codedriver.module.knowledge.dto.KnowledgeDocumentVo;

public interface KnowledgeDocumentMapper {

    public KnowledgeDocumentVo getKnowledgeDocumentById(Long id);

    public int updateKnowledgeDocumentToDeleteById(Long knowledgeDocumentId);

}
