package neatlogic.module.knowledge.auth.label;

import neatlogic.framework.auth.core.AuthBase;

import java.util.Arrays;
import java.util.List;

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

	@Override
	public List<Class<? extends AuthBase>> getIncludeAuths() {
		return Arrays.asList(KNOWLEDGE.class);
	}
}
