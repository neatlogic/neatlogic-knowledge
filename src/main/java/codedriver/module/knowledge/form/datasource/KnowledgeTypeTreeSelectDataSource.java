/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.knowledge.form.datasource;

import codedriver.framework.form.treeselect.core.TreeSelectDataSourceBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

/**
 * @author lvzk
 * @since 2021/8/11 17:46
 **/
@Component
public class KnowledgeTypeTreeSelectDataSource extends TreeSelectDataSourceBase {
    /**
     * 获取组件英文名
     *
     * @return 组件英文名
     */
    @Override
    public String getHandler() {
        return "knowledgeType";
    }

    /**
     * 获取组件中文名
     *
     * @return 组件中文名
     */
    @Override
    public String getHandlerName() {
        return "知识圈类型";
    }

    /**
     * 获取数据源配置
     *
     * @return 配置
     */
    @Override
    public JSONObject getConfig() {
        return new JSONObject(){{
            put("url","knowledge/document/type/tree/forselect");
        }};
    }
}
