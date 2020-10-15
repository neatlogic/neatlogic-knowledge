package codedriver.module.knowledge.service;

import codedriver.module.knowledge.dto.KnowledgeTypeVo;

public interface KnowledgeTypeService {
	
	public void rebuildLeftRightCode();

	public KnowledgeTypeVo buildRootKnowledgeType();

}
