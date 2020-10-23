package codedriver.module.knowledge.api.document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
@Service
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class KnowledgeDocumentSearchApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    
    @Override
    public String getToken() {
        return "knowledge/document/search";
    }

    @Override
    public String getName() {
        return "搜索文档";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({
        @Param(name = "keyword", type = ApiParamType.STRING, isRequired = true, desc = "搜索关键字"),
        @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页数", isRequired = false),
        @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目", isRequired = false)
    })
    @Output({
        @Param(name="versionid", type = ApiParamType.INTEGER, desc="版本号"),
        @Param(name="circleName", type = ApiParamType.STRING, desc="知识圈名称"),
        @Param(name="title", type = ApiParamType.STRING, desc="知识标题"),
        @Param(name="content", type = ApiParamType.STRING, desc="知识内容"),
        @Param(name="lcu", type = ApiParamType.STRING, desc="知识创建人"),
        @Param(name="lcd", type = ApiParamType.STRING, desc="知识创建时间"),
        @Param(name="tagList", type = ApiParamType.JSONARRAY, desc="知识标签"),
        @Param(name="browseCount", type = ApiParamType.LONG, desc="知识浏览量"),
        @Param(name="favorCount", type = ApiParamType.JSONARRAY, desc="知识点赞量"),
        @Param(name="collectCount", type = ApiParamType.JSONARRAY, desc="知识收藏量"),
        @Param(name="tagList", type = ApiParamType.JSONARRAY, desc="知识标签"),
        @Param(name="templateType", type = ApiParamType.STRING, desc="知识模板类型"),
        @Param(name="rowNum", type = ApiParamType.INTEGER, desc="总数"),
        @Param(name="pageSize", type = ApiParamType.INTEGER, desc="每页数据条目"),
        @Param(name="currentPage", type = ApiParamType.INTEGER, desc="当前页数"),
        @Param(name="pageCount", type = ApiParamType.INTEGER, desc="总页数"),
    })
    
    @Description(desc = "搜索文档")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
       
        return null;
    }

}
