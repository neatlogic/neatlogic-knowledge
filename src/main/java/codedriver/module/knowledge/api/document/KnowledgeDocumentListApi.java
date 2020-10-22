package codedriver.module.knowledge.api.document;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentListApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    
    @Autowired
    private UserMapper userMapper;

    @Override
    public String getToken() {
        return "knowledge/document/list";
    }

    @Override
    public String getName() {
        return "查询文档列表";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({
        @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字，匹配名称"),
        @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true"),
        @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页条目"),
        @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
        @Param(name = "knowledgeDocumentTypeUuid", type = ApiParamType.STRING, isRequired = true, desc = "类型id")
    })
    @Output({
        @Param(explode = BasePageVo.class),
        @Param(name = "theadList", type = ApiParamType.JSONARRAY, desc = "表头列表"),
        @Param(name = "tbodyList", explode = KnowledgeDocumentVersionVo[].class, desc = "文档列表")
    })
    @Description(desc = "查询文档列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        resultObj.put("theadList", getTheadList());
        resultObj.put("tbodyList", new ArrayList<>());
        KnowledgeDocumentVo searchVo = JSON.toJavaObject(jsonObj, KnowledgeDocumentVo.class);
        int pageCount = 0;
        if(searchVo.getNeedPage()) {
            int rowNum = knowledgeDocumentMapper.getKnowledgeDocumentCountByKnowledgeDocumentTypeUuid(searchVo);
            pageCount = PageUtil.getPageCount(rowNum, searchVo.getPageSize());
            resultObj.put("currentPage", searchVo.getCurrentPage());
            resultObj.put("pageSize", searchVo.getPageSize());
            resultObj.put("pageCount", pageCount);
            resultObj.put("rowNum", rowNum);
        }
        if(!searchVo.getNeedPage() || searchVo.getCurrentPage() <= pageCount) {
            UserVo currentUserVo = userMapper.getUserBaseInfoByUuid(UserContext.get().getUserUuid(true));
            List<KnowledgeDocumentVersionVo> knowledgeDocumentVersionList = knowledgeDocumentMapper.getKnowledgeDocumentListByKnowledgeDocumentTypeUuid(searchVo);
            for(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo : knowledgeDocumentVersionList) {
                knowledgeDocumentVersionVo.setLcuName(currentUserVo.getUserName());
                knowledgeDocumentVersionVo.setLcuInfo(currentUserVo.getUserInfo());
            }
            resultObj.put("tbodyList", knowledgeDocumentVersionList);
        }
        return resultObj;
    }

    @SuppressWarnings({"serial"})
    private JSONArray getTheadList() {
        JSONArray theadList = new JSONArray();
        theadList.add(new JSONObject() {{this.put("title", "标题"); this.put("key", "title");}});
        theadList.add(new JSONObject() {{this.put("title", "提交人"); this.put("key", "lcu");}});
        theadList.add(new JSONObject() {{this.put("title", "修改时间"); this.put("key", "lcd");}});
        theadList.add(new JSONObject() {{this.put("title", "大小"); this.put("key", "size");}});
        theadList.add(new JSONObject() {{this.put("key", "action");}});
        return theadList;
    }

}
