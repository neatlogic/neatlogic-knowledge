/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.knowledge.linehandler.handler;

import neatlogic.framework.knowledge.linehandler.core.KnowledgeLineHandlerBase;
import neatlogic.framework.lcs.BaseLineVo;
import org.springframework.stereotype.Component;

/**
 * @author lvzk
 * @since 2021/8/9 18:48
 **/
@Component
public class OlLineHandler extends KnowledgeLineHandlerBase {
    /**
     * 获取组件英文名
     *
     * @return 组件英文名
     */
    @Override
    public String getHandler() {
        return "ol";
    }

    /**
     * 获取组件中文名
     *
     * @return 组件中文名
     */
    @Override
    public String getHandlerName() {
        return "有序列表";
    }

    /**
     * 获取组件mainBody content|config
     *
     * @param line 行对象
     * @return mainBody content|config
     */
    @Override
    public String getMainBody(BaseLineVo line) {
        return line.getContent();
    }

    /**
     * 设置组件mainBody content|config
     *
     * @param line     行对象
     * @param mainBody content|config
     */
    @Override
    public void setMainBody(BaseLineVo line, String mainBody) {
        line.setContent(mainBody);
    }

    @Override
    public boolean needCompare() {
        return true;
    }
}
