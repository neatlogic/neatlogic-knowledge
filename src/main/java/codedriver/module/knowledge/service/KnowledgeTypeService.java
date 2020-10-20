package codedriver.module.knowledge.service;

import codedriver.module.knowledge.dto.KnowledgeTypeVo;

public interface KnowledgeTypeService {
	
	public void rebuildLeftRightCode(Long knowledgeCircleId);

	public KnowledgeTypeVo buildRootKnowledgeType(Long knowledgeCircleId);

}
