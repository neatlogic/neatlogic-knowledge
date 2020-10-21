package codedriver.module.knowledge.api.document;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentListForInternalLinkApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Override
    public String getToken() {
        return "knowledge/document/list/forinternallink";
    }

    @Override
    public String getName() {
        return "查询文档列表（用于内连接）";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({
        @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字，匹配名称"),
        @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true"),
        @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页条目"),
        @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页")
    })
    @Output({
        @Param(explode = BasePageVo.class),
        @Param(name = "list", explode = ValueTextVo[].class, desc = "文档列表")
    })
    @Description(desc = "查询文档列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        resultObj.put("list", new ArrayList<>());
        BasePageVo searchVo = JSON.toJavaObject(jsonObj, BasePageVo.class);
        int pageCount = 0;
        if(searchVo.getNeedPage()) {
            int rowNum = knowledgeDocumentMapper.getKnowledgeDocumentCountForInternalLink(searchVo);
            pageCount = PageUtil.getPageCount(rowNum, searchVo.getPageSize());
            resultObj.put("currentPage", searchVo.getCurrentPage());
            resultObj.put("pageSize", searchVo.getPageSize());
            resultObj.put("pageCount", pageCount);
            resultObj.put("rowNum", rowNum);
        }
        if(!searchVo.getNeedPage() || searchVo.getCurrentPage() <= pageCount) {
            List<ValueTextVo> knowledgeDocumentList = knowledgeDocumentMapper.getKnowledgeDocumentListForInternalLink(searchVo);
            resultObj.put("list", knowledgeDocumentList);
        }
        return resultObj;
    }

}
