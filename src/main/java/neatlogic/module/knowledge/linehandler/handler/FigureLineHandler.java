/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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

import neatlogic.framework.knowledge.constvalue.KnowledgeDocumentLineHandler;
import neatlogic.framework.lcs.BaseLineVo;
import neatlogic.framework.knowledge.linehandler.core.KnowledgeLineHandlerBase;
import neatlogic.framework.lcs.linehandler.core.LineHandlerFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * @author lvzk
 * @since 2021/8/9 18:48
 **/
@Component
public class FigureLineHandler extends KnowledgeLineHandlerBase {
    /**
     * 获取组件英文名
     *
     * @return 组件英文名
     */
    @Override
    public String getHandler() {
        return "figure";
    }

    /**
     * 获取组件中文名
     *
     * @return 组件中文名
     */
    @Override
    public String getHandlerName() {
        return "流内容";
    }

    /**
     * 获取组件mainBody content|config
     *
     * @param line 行对象
     * @return mainBody content|config
     */
    @Override
    public String getMainBody(BaseLineVo line) {
        return null;
    }

    /**
     * 设置组件mainBody content|config
     *
     * @param line     行对象
     * @param mainBody content|config
     */
    @Override
    public void setMainBody(BaseLineVo line, String mainBody) {

    }

    @Override
    public boolean needCompare() {
        return false;
    }

    @Override
    public String myConvertHtmlToConfig(Element element) {
        Elements elements = element.getElementsByTag(KnowledgeDocumentLineHandler.TABLE.getValue());
        if (CollectionUtils.isNotEmpty(elements)) {
            return ((KnowledgeLineHandlerBase) LineHandlerFactory.getHandler(KnowledgeDocumentLineHandler.TABLE.getValue())).convertHtmlToConfig(elements.get(0));
        }
        elements = element.getElementsByTag(KnowledgeDocumentLineHandler.IMG.getValue());
        if (CollectionUtils.isNotEmpty(elements)) {
            return ((KnowledgeLineHandlerBase) LineHandlerFactory.getHandler(KnowledgeDocumentLineHandler.IMG.getValue())).convertHtmlToConfig(elements.get(0));
        }
        return null;
    }

    @Override
    public String myConvertHtmlToContent(Element element) {
        Elements elements = element.getElementsByTag(KnowledgeDocumentLineHandler.TABLE.getValue());
        if (CollectionUtils.isNotEmpty(elements)) {
            return ((KnowledgeLineHandlerBase) LineHandlerFactory.getHandler(KnowledgeDocumentLineHandler.TABLE.getValue())).convertHtmlToContent(elements.get(0));
        }
        elements = element.getElementsByTag(KnowledgeDocumentLineHandler.IMG.getValue());
        if (CollectionUtils.isNotEmpty(elements)) {
            return ((KnowledgeLineHandlerBase) LineHandlerFactory.getHandler(KnowledgeDocumentLineHandler.IMG.getValue())).convertHtmlToContent(elements.get(0));
        }
        return null;
    }

    @Override
    public String myRealHandler(Element element){
        Elements elements = element.getElementsByTag(KnowledgeDocumentLineHandler.TABLE.getValue());
        if (CollectionUtils.isNotEmpty(elements)) {
            return ((KnowledgeLineHandlerBase) LineHandlerFactory.getHandler(KnowledgeDocumentLineHandler.TABLE.getValue())).getRealHandler(elements.get(0));
        }
        elements = element.getElementsByTag(KnowledgeDocumentLineHandler.IMG.getValue());
        if (CollectionUtils.isNotEmpty(elements)) {
            return ((KnowledgeLineHandlerBase) LineHandlerFactory.getHandler(KnowledgeDocumentLineHandler.IMG.getValue())).getRealHandler(elements.get(0));
        }
        return KnowledgeDocumentLineHandler.P.getValue();
    }
}
