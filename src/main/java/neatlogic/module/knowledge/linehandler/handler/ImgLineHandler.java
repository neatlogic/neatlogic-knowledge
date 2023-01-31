/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.knowledge.linehandler.handler;

import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.knowledge.constvalue.KnowledgeDocumentLineHandler;
import neatlogic.framework.lcs.BaseLineVo;
import neatlogic.framework.knowledge.linehandler.core.KnowledgeLineHandlerBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author lvzk
 * @since 2021/8/9 18:48
 **/
@Component
public class ImgLineHandler extends KnowledgeLineHandlerBase {
    @Resource
    FileMapper fileMapper;

    /**
     * 获取组件英文名
     *
     * @return 组件英文名
     */
    @Override
    public String getHandler() {
        return "img";
    }

    /**
     * 获取组件中文名
     *
     * @return 组件中文名
     */
    @Override
    public String getHandlerName() {
        return "图片";
    }

    /**
     * 获取组件mainBody content|config
     *
     * @param line 行对象
     * @return mainBody content|config
     */
    @Override
    public String getMainBody(BaseLineVo line) {
        return line.getConfig().getString("url");
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
    protected String myConvertHtmlToConfig(Element element) {
        JSONObject imgJson = new JSONObject();
        String src = element.attr("src");
        Long fileId = null;
        String regular = "[\\?|\\&]?id=([^&]*)";
        Pattern p = Pattern.compile(regular);
        Matcher m = p.matcher(src);
        boolean result = m.find();
        while (result) {
            fileId = Long.valueOf(m.group(1));
            result = m.find();
        }
        if(fileId != null){
            FileVo file = fileMapper.getFileById(fileId);
            imgJson.put("name",file.getName());
            imgJson.put("title",file.getName());
            imgJson.put("align","left");
            imgJson.put("value", StringUtils.EMPTY);
            imgJson.put("url",src);
            return imgJson.toString();
        }else{
            return null;
        }
    }

    @Override
    public String myConvertHtmlToContent(Element element) {
        String src = element.attr("src");
        String regular = "[\\?|\\&]?id=([^&]*)";
        Pattern p = Pattern.compile(regular);
        Matcher m = p.matcher(src);
        boolean result = m.find();
        if(!result){
            return element.outerHtml();
        }
        return null;
    }

    @Override
    public String myRealHandler(Element element){
        String src = element.attr("src");
        String regular = "[\\?|\\&]?id=([^&]*)";
        Pattern p = Pattern.compile(regular);
        Matcher m = p.matcher(src);
        boolean result = m.find();
        if(!result){
            return KnowledgeDocumentLineHandler.P.getValue();
        }
        return KnowledgeDocumentLineHandler.IMG.getValue();
    }
}
