package neatlogic.module.knowledge.auth.label;

import neatlogic.framework.auth.core.AuthBase;
import neatlogic.framework.knowledge.auth.label.KNOWLEDGE;

import java.util.Arrays;
import java.util.List;

/** 权限名称与说明使用国际化键，权限标识及校验规则保持不变。 */
public class KNOWLEDGE_BASE extends AuthBase {

	@Override
	public String getAuthDisplayName() {
		return "auth.knowledge_base.name";
	}

	@Override
	public String getAuthIntroduction() {
		return "auth.knowledge_base.description";
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
