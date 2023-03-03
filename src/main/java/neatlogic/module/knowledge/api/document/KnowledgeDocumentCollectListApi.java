package neatlogic.module.knowledge.api.document;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentCollectVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVersionVo;
import neatlogic.module.knowledge.service.KnowledgeDocumentService;

@Deprecated
//@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentCollectListApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @Override
    public String getToken() {
        return "knowledge/document/collect/list";
    }

    @Override
    public String getName() {
        return "查询收藏列表";
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
        @Param(name = "theadList", type = ApiParamType.JSONARRAY, desc = "表头列表"),
        @Param(name = "tbodyList", explode = KnowledgeDocumentVersionVo[].class, desc = "收藏列表")
    })
    @Description(desc = "查询收藏列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        resultObj.put("theadList", getTheadList());
        resultObj.put("tbodyList", new ArrayList<>());
        KnowledgeDocumentCollectVo searchVo = JSON.toJavaObject(jsonObj, KnowledgeDocumentCollectVo.class);
        searchVo.setUserUuid(UserContext.get().getUserUuid(true));
        int pageCount = 0;
        if(searchVo.getNeedPage()) {
            int rowNum = knowledgeDocumentMapper.getKnowledgeDocumentVersionMyCollectCount(searchVo);
            pageCount = PageUtil.getPageCount(rowNum, searchVo.getPageSize());
            resultObj.put("currentPage", searchVo.getCurrentPage());
            resultObj.put("pageSize", searchVo.getPageSize());
            resultObj.put("pageCount", pageCount);
            resultObj.put("rowNum", rowNum);
        }
        if(!searchVo.getNeedPage() || searchVo.getCurrentPage() <= pageCount) {
            List<KnowledgeDocumentVersionVo> knowledgeDocumentVersionList = knowledgeDocumentMapper.getKnowledgeDocumentVersionMyCollectList(searchVo);
            for(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo : knowledgeDocumentVersionList) {
                knowledgeDocumentVersionVo.setIsDeletable(knowledgeDocumentService.isDeletable(knowledgeDocumentVersionVo));
                knowledgeDocumentVersionVo.setIsEditable(1);
            }
            resultObj.put("tbodyList", knowledgeDocumentVersionList);
        }
        return resultObj;
    }

    @SuppressWarnings({"serial"})
    private JSONArray getTheadList() {
        JSONArray theadList = new JSONArray();
        theadList.add(new JSONObject() {{this.put("title", "标题"); this.put("key", "title");}});
        theadList.add(new JSONObject() {{this.put("title", "创建人"); this.put("key", "fcuName");}});
        theadList.add(new JSONObject() {{this.put("title", "最后修改人"); this.put("key", "lcuName");}});
        theadList.add(new JSONObject() {{this.put("title", "最后一次审批时间"); this.put("key", "reviewerTime");}});
        theadList.add(new JSONObject() {{this.put("key", "action");}});
        return theadList;
    }

}
