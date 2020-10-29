package codedriver.module.knowledge.constvalue;

import java.util.function.BiConsumer;
import java.util.function.Function;

//import com.alibaba.fastjson.JSON;

import codedriver.module.knowledge.dto.KnowledgeDocumentLineVo;

public enum KnowledgeDocumentLineHandler {

    P("p", "段落", (line) -> line.getContent(), (line, mainBody) -> line.setContent(mainBody)),
    H1("h1", "一级标题", (line) -> line.getContent(), (line, mainBody) -> line.setContent(mainBody)),
    H2("h2", "二级标题", (line) -> line.getContent(), (line, mainBody) -> line.setContent(mainBody)),
    IMG("img", "图片", (line) -> null, null),
//    TABLE("table", "表格", (line) -> JSON.toJSONString(line.getConfig().getJSONArray("tableList")), (line, mainBody) -> line.getConfig().put("tableList", JSON.parseArray(mainBody))),
    TABLE("table", "表格", (line) -> null, null),
    CODE("code", "代码块", (line) -> null, null),
    UL("ul", "无序列表", (line) -> line.getContent(), (line, mainBody) -> line.setContent(mainBody)),
    OL("ol", "有序列表", (line) -> line.getContent(), (line, mainBody) -> line.setContent(mainBody))
    ;
    private String value;
    private String text;
    private Function<KnowledgeDocumentLineVo, String> getMainBody;
    private BiConsumer<KnowledgeDocumentLineVo, String> setMainBody;
    private KnowledgeDocumentLineHandler(String value, String text, Function<KnowledgeDocumentLineVo, String> getMainBody, BiConsumer<KnowledgeDocumentLineVo, String> setMainBody) {
        this.value = value;
        this.text = text;
        this.getMainBody = getMainBody;
        this.setMainBody = setMainBody;
    }
    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
    public static String getMainBody(KnowledgeDocumentLineVo line){
        for(KnowledgeDocumentLineHandler handler : values()) {
            if(handler.value.equals(line.getHandler())) {
                if(handler.getMainBody != null) {
                    return handler.getMainBody.apply(line); 
                }
                return null;
            }
        }
        return null;
    }
    public static void setMainBody(KnowledgeDocumentLineVo line, String mainBody){
        for(KnowledgeDocumentLineHandler handler : values()) {
            if(handler.value.equals(line.getHandler())) {
                if(handler.setMainBody != null) {
                    handler.setMainBody.accept(line, mainBody);
                }
                return;
            }
        }
    }
}
