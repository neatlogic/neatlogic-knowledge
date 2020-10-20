package codedriver.module.knowledge.exception;

import codedriver.framework.exception.core.ApiRuntimeException;

public class KnowledgeTypeNotFoundException extends ApiRuntimeException {

	private static final long serialVersionUID = -6399417451182542331L;

	public KnowledgeTypeNotFoundException(String uuid) {
		super("知识类型：" + uuid + "不存在");
	}


}
