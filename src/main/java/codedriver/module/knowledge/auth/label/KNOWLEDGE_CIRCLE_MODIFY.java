package codedriver.module.knowledge.auth.label;

import codedriver.framework.auth.core.AuthBase;

public class KNOWLEDGE_CIRCLE_MODIFY extends AuthBase {

	@Override
	public String getAuthDisplayName() {
		return "知识圈管理权限";
	}

	@Override
	public String getAuthIntroduction() {
		return "对知识圈进行添加、修改和删除";
	}

	@Override
	public String getAuthGroup() {
		return "knowledge";
	}
}
