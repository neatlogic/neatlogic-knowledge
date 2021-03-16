package codedriver.module.knowledge.exception;

import codedriver.framework.exception.core.ApiRuntimeException;

public class KnowledgeCircleHasKnowledgeException extends ApiRuntimeException {

	private static final long serialVersionUID = -380236429435380456L;

	public KnowledgeCircleHasKnowledgeException(Long id) {
		super("知识圈：'" + id + "'中已存在文档，不可删除");
	}


}
