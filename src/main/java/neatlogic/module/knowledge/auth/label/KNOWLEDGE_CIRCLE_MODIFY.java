package neatlogic.module.knowledge.auth.label;

import neatlogic.framework.auth.core.AuthBase;

import java.util.Collections;
import java.util.List;

public class KNOWLEDGE_CIRCLE_MODIFY extends AuthBase {

	@Override
	public String getAuthDisplayName() {
		return "auth.knowledge.knowledgecirclemodify.name";
	}

	@Override
	public String getAuthIntroduction() {
		return "auth.knowledge.knowledgecirclemodify.introduction";
	}

	@Override
	public String getAuthGroup() {
		return "knowledge";
	}

	@Override
	public Integer getSort() {
		return 2;
	}

	@Override
	public List<Class<? extends AuthBase>> getIncludeAuths(){
		return Collections.singletonList(KNOWLEDGE_BASE.class);
	}
}
