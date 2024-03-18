/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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

import neatlogic.framework.lcs.BaseLineVo;
import neatlogic.framework.knowledge.linehandler.core.KnowledgeLineHandlerBase;
import org.springframework.stereotype.Component;

/**
 * @author lvzk
 * @since 2021/8/9 18:48
 **/
@Component
public class EditorLineHandler extends KnowledgeLineHandlerBase {
    /**
     * 获取组件英文名
     *
     * @return 组件英文名
     */
    @Override
    public String getHandler() {
        return "editor";
    }

    /**
     * 获取组件中文名
     *
     * @return 组件中文名
     */
    @Override
    public String getHandlerName() {
        return "编辑器";
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

    @Override
    protected String myConvertContentToHtml(BaseLineVo line) {
        /* editor无论内容如何都要独占一行，加上P标签能保证始终独占一行 */
        return line.getContent() != null ? "<p>" + line.getContent() + "</p>" : "</br>";
    }
}
