package codedriver.module.knowledge.auth.label;

import codedriver.framework.auth.core.AuthBase;

public class KNOWLEDGE_BASE extends AuthBase {

	@Override
	public String getAuthDisplayName() {
		return "知识基础权限";
	}

	@Override
	public String getAuthIntroduction() {
		return "查看知识";
	}

	@Override
	public String getAuthGroup() {
		return "knowledge";
	}

	@Override
	public Integer getSort() {
		return 1;
	}
}
