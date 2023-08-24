/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
