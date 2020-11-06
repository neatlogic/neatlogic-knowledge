package codedriver.module.knowledge.constvalue;

import java.util.function.BiConsumer;
import java.util.function.Function;

//import com.alibaba.fastjson.JSON;

import codedriver.module.knowledge.dto.KnowledgeDocumentLineVo;

public enum KnowledgeDocumentLineHandler {

    P("p", "段落", (line) -> line.getContent(), (line, mainBody) -> line.setContent(mainBody)),
    H1("h1", "一级标题", (line) -> line.getContent(), (line, mainBody) -> line.setContent(mainBody)),
    H2("h2", "二级标题", (line) -> line.getContent(), (line, mainBody) -> line.setContent(mainBody)),
    IMG("img", "图片", (line) -> line.getConfig().getString("url"), null),
    TABLE("table", "表格", (line) -> line.getConfig().getString("tableList"), null),
//    TABLE("table", "表格", (line) -> null, (line, mainBody) -> line.getConfig().put("tableList", JSON.parseArray(mainBody))),
//    CODE("code", "代码块", (line) -> line.getConfig().getString("value"), (line, mainBody) -> line.getConfig().put("value", mainBody)),
    CODE("code", "代码块", (line) -> line.getConfig().getString("value"), null),
    FORMTABLE("formtable", "表单", (line) -> line.getContent(), null),
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
}
