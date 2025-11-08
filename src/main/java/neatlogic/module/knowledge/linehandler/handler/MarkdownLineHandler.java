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

@Component
public class MarkdownLineHandler extends KnowledgeLineHandlerBase {
    @Override
    public String getHandler() {
        return "markdown";
    }

    @Override
    public String getHandlerName() {
        return "markdown";
    }

    @Override
    public String getMainBody(BaseLineVo line) {
        return line.getContent();
    }

    @Override
    public void setMainBody(BaseLineVo line, String mainBody) {
        line.setContent(mainBody);
    }

    @Override
    public boolean needCompare() {
        return true;
    }
}
