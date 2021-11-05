/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.knowledge.linehandler.handler;

import codedriver.framework.knowledge.dto.KnowledgeDocumentLineVo;
import codedriver.framework.knowledge.linehandler.core.LineHandlerBase;
import codedriver.framework.util.HtmlUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.itextpdf.tool.xml.html.HTML;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * @author lvzk
 * @since 2021/8/9 18:48
 **/
@Component
public class TableLineHandler extends LineHandlerBase {
    /**
     * 获取组件英文名
     *
     * @return 组件英文名
     */
    @Override
    public String getHandler() {
        return "table";
    }

    /**
     * 获取组件中文名
     *
     * @return 组件中文名
     */
    @Override
    public String getHandlerName() {
        return "表格";
    }

    /**
     * 获取组件mainBody content|config
     *
     * @param line 行对象
     * @return mainBody content|config
     */
    @Override
    public String getMainBody(KnowledgeDocumentLineVo line) {
        return line.getConfig().getString("tableList");
    }

    /**
     * 设置组件mainBody content|config
     *
     * @param line     行对象
     * @param mainBody content|config
     */
    @Override
    public void setMainBody(KnowledgeDocumentLineVo line, String mainBody) {

    }

    @Override
    public boolean needCompare() {
        return false;
    }

    @Override
    protected String myConvertContentToHtml(KnowledgeDocumentLineVo line) {
        JSONObject config = line.getConfig();
        JSONArray tableList = config.getJSONArray("tableList");
        JSONObject tableStyleConfig = config.getJSONObject("tableStyle");
        String tableStyle = "table-layout:fixed;border-collapse:collapse;width:100%;text-align:left;";
        String tdStyle = "border-bottom:1px solid grey";
        String trStyle = "height:42px";
        if (MapUtils.isNotEmpty(tableStyleConfig)) {
            tableStyle = StringUtils.isNotBlank(tableStyleConfig.getString("table")) ? tableStyleConfig.getString("table") : tableStyle;
            tdStyle = StringUtils.isNotBlank(tableStyleConfig.getString("td")) ? tableStyleConfig.getString("td") : tdStyle;
            trStyle = StringUtils.isNotBlank(tableStyleConfig.getString("tr")) ? tableStyleConfig.getString("tr") : trStyle;
        }
        StringBuilder sb = new StringBuilder();
        if (CollectionUtils.isNotEmpty(tableList)) {
            sb.append("<table style=\"" + tableStyle + "\">");
            sb.append("<tbody>");
            for (int i = 0; i < tableList.size(); i++) {
                sb.append("<tr style=\"" + trStyle + "\">");
                JSONArray row = tableList.getJSONArray(i);
                for (int j = 0; j < row.size(); j++) {
                    sb.append("<td style=\"" + tdStyle + "\">" + row.getString(j) + "</td>");
                }
                sb.append("</tr>");
            }
            sb.append("</tbody>");
            sb.append("</table>");
        }
        return sb.toString();
    }

    @Override
    protected String myConvertHtmlToConfig(Element element) {
        JSONObject tableJson = new JSONObject();
        Elements trElements = element.getElementsByTag(HTML.Tag.TR);
        tableJson.put("headerList", CollectionUtils.EMPTY_COLLECTION);
        tableJson.put("mergeData", CollectionUtils.EMPTY_COLLECTION);
        tableJson.put("lefterList", CollectionUtils.EMPTY_COLLECTION);
        String[][] tableData = new String[trElements.size()][];
        for (int i = 0; i < trElements.size(); i++) {
            Elements tdElements = trElements.get(i).getElementsByTag(HTML.Tag.TD);
            tableData[i] = new String[tdElements.size()];
            for (int j = 0; j < tdElements.size(); j++) {
                Elements spanElements = tdElements.get(j).getElementsByTag(HTML.Tag.SPAN);
                if (CollectionUtils.isNotEmpty(spanElements)) {
                    tableData[i][j] = HtmlUtil.decodeHtml(spanElements.get(0).html());
                } else {
                    tableData[i][j] = HtmlUtil.decodeHtml(tdElements.get(j).html());
                }
            }
        }
        tableJson.put("tableList", tableData);
        return tableJson.toString();

    }

    @Override
    public String myConvertHtmlToContent(Element element) {
        return null;
    }

}
