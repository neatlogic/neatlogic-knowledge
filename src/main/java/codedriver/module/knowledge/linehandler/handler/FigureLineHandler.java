/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.knowledge.linehandler.handler;

import codedriver.framework.knowledge.constvalue.KnowledgeDocumentLineHandler;
import codedriver.framework.lcs.BaseLineVo;
import codedriver.framework.knowledge.linehandler.core.KnowledgeLineHandlerBase;
import codedriver.framework.lcs.linehandler.core.LineHandlerFactory;
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
