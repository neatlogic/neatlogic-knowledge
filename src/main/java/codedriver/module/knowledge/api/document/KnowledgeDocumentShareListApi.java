package codedriver.module.knowledge.api.document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

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
import codedriver.module.knowledge.constvalue.KnowledgeDocumentVersionStatus;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.service.KnowledgeDocumentService;

//@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentShareListApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;
    
    @Override
    public String getToken() {
        return "knowledge/document/share/list";
    }

    @Override
    public String getName() {
        return "查询共享列表";
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
        @Param(name = "tbodyList", explode = KnowledgeDocumentVersionVo[].class, desc = "文档版本列表")
    })
    @Description(desc = "查询共享列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        resultObj.put("theadList", getTheadList());
        resultObj.put("tbodyList", new ArrayList<>());
        List<String> statusList = Arrays.asList(KnowledgeDocumentVersionStatus.PASSED.getValue(), KnowledgeDocumentVersionStatus.REJECTED.getValue(), KnowledgeDocumentVersionStatus.SUBMITTED.getValue());
        KnowledgeDocumentVersionVo searchVo = JSON.toJavaObject(jsonObj, KnowledgeDocumentVersionVo.class);
        searchVo.setLcu(UserContext.get().getUserUuid(true));
        searchVo.setStatusList(statusList);
        int pageCount = 0;
        if(searchVo.getNeedPage()) {
            int rowNum = knowledgeDocumentMapper.getKnowledgeDocumentVersionMyVersionCount(searchVo);
            pageCount = PageUtil.getPageCount(rowNum, searchVo.getPageSize());
            resultObj.put("currentPage", searchVo.getCurrentPage());
            resultObj.put("pageSize", searchVo.getPageSize());
            resultObj.put("pageCount", pageCount);
            resultObj.put("rowNum", rowNum);
        }
        if(!searchVo.getNeedPage() || searchVo.getCurrentPage() <= pageCount) {
            UserVo currentUserVo = userMapper.getUserBaseInfoByUuid(UserContext.get().getUserUuid(true));
            List<KnowledgeDocumentVersionVo> knowledgeDocumentVersionList = knowledgeDocumentMapper.getKnowledgeDocumentVersionMyVersionList(searchVo);
            for(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo : knowledgeDocumentVersionList) {
                knowledgeDocumentVersionVo.setLcuName(currentUserVo.getUserName());
                knowledgeDocumentVersionVo.setLcuInfo(currentUserVo.getUserInfo());
                if(StringUtils.isNotBlank(knowledgeDocumentVersionVo.getReviewer())) {
                    UserVo reviewerUserVo = userMapper.getUserBaseInfoByUuid(knowledgeDocumentVersionVo.getReviewer());
                    knowledgeDocumentVersionVo.setReviewerName(reviewerUserVo.getUserName());
                    knowledgeDocumentVersionVo.setReviewerInfo(reviewerUserVo.getUserInfo());
                }
                knowledgeDocumentVersionVo.setIsDeletable(knowledgeDocumentService.isDeletable(knowledgeDocumentVersionVo));
                knowledgeDocumentVersionVo.setIsEditable(knowledgeDocumentService.isEditable(knowledgeDocumentVersionVo));
            }
            resultObj.put("tbodyList", knowledgeDocumentVersionList);
        }
        return resultObj;
    }

    @SuppressWarnings({"serial"})
    private JSONArray getTheadList() {
        JSONArray theadList = new JSONArray();
        theadList.add(new JSONObject() {{this.put("title", "标题"); this.put("key", "title");}});
        theadList.add(new JSONObject() {{this.put("title", "提交人"); this.put("key", "lcuName");}});
        theadList.add(new JSONObject() {{this.put("title", "提交时间"); this.put("key", "lcd");}});
        theadList.add(new JSONObject() {{this.put("title", "审核人"); this.put("key", "reviewer");}});
        theadList.add(new JSONObject() {{this.put("title", "审核时间"); this.put("key", "reviewTime");}});
        theadList.add(new JSONObject() {{this.put("title", "状态"); this.put("key", "statusVo.text");}});
        theadList.add(new JSONObject() {{this.put("key", "action");}});
        return theadList;
    }

}
