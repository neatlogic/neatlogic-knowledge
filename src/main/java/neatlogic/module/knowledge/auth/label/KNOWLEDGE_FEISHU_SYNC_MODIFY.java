package neatlogic.module.knowledge.auth.label;

import neatlogic.framework.auth.core.AuthBase;

import java.util.Collections;
import java.util.List;

/** 权限名称与说明使用国际化键，权限标识及校验规则保持不变。 */
public class KNOWLEDGE_FEISHU_SYNC_MODIFY extends AuthBase {

    @Override
    public String getAuthDisplayName() {
        return "auth.knowledge_feishu_sync_modify.name";
    }

    @Override
    public String getAuthIntroduction() {
        return "auth.knowledge_feishu_sync_modify.description";
    }

    @Override
    public String getAuthGroup() {
        return "knowledge";
    }

    @Override
    public Integer getSort() {
        return 4;
    }

    @Override
    public List<Class<? extends AuthBase>> getIncludeAuths() {
        return Collections.singletonList(KNOWLEDGE_BASE.class);
    }
}
