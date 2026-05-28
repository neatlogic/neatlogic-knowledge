package neatlogic.module.knowledge.auth.label;

import neatlogic.framework.auth.core.AuthBase;

import java.util.Collections;
import java.util.List;

public class KNOWLEDGE_FEISHU_SYNC_MODIFY extends AuthBase {

    @Override
    public String getAuthDisplayName() {
        return "飞书云文档同步管理权限";
    }

    @Override
    public String getAuthIntroduction() {
        return "管理飞书云文档同步配置、手动同步和同步记录";
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
