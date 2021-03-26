package codedriver.module.knowledge.constvalue;

import java.util.function.BiConsumer;
import java.util.function.Function;

//import com.alibaba.fastjson.JSON;

import codedriver.module.knowledge.dto.KnowledgeDocumentLineVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;

public enum KnowledgeDocumentLineHandler {

    P("p", "段落", (line) -> line.getContent(), (line, mainBody) -> line.setContent(mainBody)),
    H1("h1", "一级标题", (line) -> line.getContent(), (line, mainBody) -> line.setContent(mainBody)),
    H2("h2", "二级标题", (line) -> line.getContent(), (line, mainBody) -> line.setContent(mainBody)),
    IMG("img", "图片", (line) -> line.getConfig().getString("url"), null),
    TABLE("table", "表格", (line) -> line.getConfig().getString("tableList"), null),
//    TABLE("table", "表格", (line) -> null, (line, mainBody) -> line.getConfig().put("tableList", JSON.parseArray(mainBody))),
//    CODE("code", "代码块", (line) -> line.getConfig().getString("value"), (line, mainBody) -> line.getConfig().put("value", mainBody)),
    CODE("code", "代码块", (line) -> line.getConfig().getString("value"), null),
    FORMTABLE("formtable", "表单", (line) -> line.getContent(), (line, mainBody) -> line.setContent(mainBody)),
    EDITOR("editor", "编辑器", (line) -> line.getContent(), (line, mainBody) -> line.setContent(mainBody)),
    UL("ul", "无序列表", (line) -> line.getContent(), (line, mainBody) -> line.setContent(mainBody)),
    OL("ol", "有序列表", (line) -> line.getContent(), (line, mainBody) -> line.setContent(mainBody))
    ;
    private String value;
    private String text;
    private Function<KnowledgeDocumentLineVo, String> mainBodyGet;
    private BiConsumer<KnowledgeDocumentLineVo, String> mainBodySet;
    private KnowledgeDocumentLineHandler(String value, String text, Function<KnowledgeDocumentLineVo, String> mainBodyGet, BiConsumer<KnowledgeDocumentLineVo, String> mainBodySet) {
        this.value = value;
        this.text = text;
        this.mainBodyGet = mainBodyGet;
        this.mainBodySet = mainBodySet;
    }
    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    public static BiConsumer<KnowledgeDocumentLineVo, String> getMainBodySet(String _value){
        for(KnowledgeDocumentLineHandler handler : values()) {
            if(handler.value.equals(_value)) {
                return handler.mainBodySet;
            }
        }
        return null;
    }
    public static String getMainBody(KnowledgeDocumentLineVo line){
        for(KnowledgeDocumentLineHandler handler : values()) {
            if(handler.value.equals(line.getHandler())) {
                if(handler.mainBodyGet != null) {
                    return handler.mainBodyGet.apply(line); 
                }
                return null;
            }
        }
        return null;
    }
    public static void setMainBody(KnowledgeDocumentLineVo line, String mainBody){
        for(KnowledgeDocumentLineHandler handler : values()) {
            if(handler.value.equals(line.getHandler())) {
                if(handler.mainBodySet != null) {
                    handler.mainBodySet.accept(line, mainBody);
                }
                return;
            }
        }
    }

    public static String convertContentToHtml(KnowledgeDocumentLineVo line){
        /** editor不在标准handler之列，只在工单转知识时才有可能出现 **/
        if("editor".equals(line.getHandler())){
            /** editor无论内容如何都要独占一行，加上P标签能保证始终独占一行 **/
            return line.getContent() != null ? "<p>" + line.getContent() + "</p>" : "</br>";
        }
        for(KnowledgeDocumentLineHandler handler : values()) {
            if(handler.value.equals(line.getHandler())) {
                if(P.value.equals(line.getHandler()) || H1.value.equals((line.getHandler()))
                || H2.value.equals(line.getHandler()) || UL.value.equals(line.getHandler())
                || OL.value.equals(line.getHandler())){
                    return "<" + handler.value + ">"
                            + (line.getContent() != null ? line.getContent() : "")
                            + "</" + handler.value + ">";
                }else if(CODE.value.equals(line.getHandler())){
                    JSONObject config = line.getConfig();
                    String code = config.getString("value");
                    if(code != null){
                        code = code.replaceAll("\\n","<br/>");
                    }
                    return "<div>\n" + (code != null ? code : "") + "</div>";
                }else if(FORMTABLE.value.equals(line.getHandler())){
                    return line.getContent();
                }else if(TABLE.value.equals(line.getHandler())){
                    JSONObject config = line.getConfig();
                    JSONArray tableList = config.getJSONArray("tableList");
                    StringBuilder sb = new StringBuilder();
                    if(CollectionUtils.isNotEmpty(tableList)) {
                        sb.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"5\" " +
                                "style=\"border: 1px solid #DBDBDB;border-collapse: collapse;\">");
                        sb.append("<tbody>");
                        for(int i = 0;i < tableList.size();i++){
                            sb.append("<tr>");
                            JSONArray row = tableList.getJSONArray(i);
                            for(int j = 0;j < row.size();j++){
                                sb.append("<td>" + row.getString(j) + "</td>");
                            }
                            sb.append("</tr>");
                        }
                        sb.append("</tbody>");
                        sb.append("</table>");
                    }
                    return sb.toString();
                }
            }
        }
        return null;
    }
}
