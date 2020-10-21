package codedriver.module.knowledge.service;

import codedriver.module.knowledge.dto.KnowledgeDocumentTypeVo;

public interface KnowledgeDocumentTypeService {
	
	public void rebuildLeftRightCode(Long knowledgeCircleId);

	public KnowledgeDocumentTypeVo buildRootType(Long knowledgeCircleId);

}
