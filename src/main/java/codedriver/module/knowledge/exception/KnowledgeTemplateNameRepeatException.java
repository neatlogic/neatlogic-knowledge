package codedriver.module.knowledge.exception;

import codedriver.framework.exception.core.ApiFieldValidRuntimeException;

public class KnowledgeTemplateNameRepeatException extends ApiFieldValidRuntimeException {

	private static final long serialVersionUID = -4894671473727349454L;

	public KnowledgeTemplateNameRepeatException(String name) {
		super("知识模版：" + name + "已存在");
	}


}
