package codedriver.module.knowledge.auth.label;

import codedriver.framework.auth.core.AuthBase;

public class KNOWLEDGE_TEMPLATE_MODIFY extends AuthBase {

	@Override
	public String getAuthDisplayName() {
		return "知识模版管理权限";
	}

	@Override
	public String getAuthIntroduction() {
		return "对知识模版进行添加、修改和删除";
	}

	@Override
	public String getAuthGroup() {
		return "knowledge";
	}

	@Override
	public Integer getSort() {
		return 3;
	}
}
