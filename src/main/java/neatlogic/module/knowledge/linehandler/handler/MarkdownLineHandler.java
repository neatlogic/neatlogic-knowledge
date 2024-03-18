/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
