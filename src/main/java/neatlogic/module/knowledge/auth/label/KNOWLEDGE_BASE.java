package neatlogic.module.knowledge.auth.label;

import neatlogic.framework.auth.core.AuthBase;

public class KNOWLEDGE_BASE extends AuthBase {

	@Override
	public String getAuthDisplayName() {
		return "auth.knowledge.knowledgebase.name";
	}

	@Override
	public String getAuthIntroduction() {
		return "auth.knowledge.knowledgebase.introduction";
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
