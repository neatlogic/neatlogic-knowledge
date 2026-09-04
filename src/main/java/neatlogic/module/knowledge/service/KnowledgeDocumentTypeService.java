package neatlogic.module.knowledge.service;

import neatlogic.framework.knowledge.dto.KnowledgeDocumentTypeVo;

public interface KnowledgeDocumentTypeService {
	
	public void rebuildLeftRightCode(Long knowledgeCircleId);

	public KnowledgeDocumentTypeVo buildRootType(Long knowledgeCircleId);

	/**
	 * 构建指定知识圈的完整分类树，返回不对外展示的虚拟分类根节点。
	 */
	KnowledgeDocumentTypeVo buildTypeTree(Long knowledgeCircleId);

}
