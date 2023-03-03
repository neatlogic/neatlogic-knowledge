package neatlogic.module.knowledge.service;

import neatlogic.framework.knowledge.dto.KnowledgeDocumentTypeVo;

public interface KnowledgeDocumentTypeService {
	
	public void rebuildLeftRightCode(Long knowledgeCircleId);

	public KnowledgeDocumentTypeVo buildRootType(Long knowledgeCircleId);

}
