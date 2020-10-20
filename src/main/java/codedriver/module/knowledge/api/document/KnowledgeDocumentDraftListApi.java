package codedriver.module.knowledge.api.document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
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

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentDraftListApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    
    @Autowired
    private UserMapper userMapper;

    @Override
    public String getToken() {
        return "knowledge/document/draft/list";
    }

    @Override
    public String getName() {
        return "查询草稿列表";
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
        @Param(name = "knowledgeDocumentVersionList", explode = KnowledgeDocumentVersionVo[].class, desc = "文档版本列表")
    })
    @Description(desc = "查询草稿列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        resultObj.put("knowledgeDocumentVersionList", new ArrayList<>());
        List<String> statusList = Arrays.asList(KnowledgeDocumentVersionStatus.DRAFT.getValue(), KnowledgeDocumentVersionStatus.EXPIRED.getValue());
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
            }
            resultObj.put("knowledgeDocumentVersionList", knowledgeDocumentVersionList);
        }
        return resultObj;
    }

}
