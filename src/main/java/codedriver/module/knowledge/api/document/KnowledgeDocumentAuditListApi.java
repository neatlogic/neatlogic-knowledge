package codedriver.module.knowledge.api.document;

import java.util.ArrayList;
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
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentAuditMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentAuditVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentAuditListApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    
    @Autowired
    private KnowledgeDocumentAuditMapper knowledgeDocumentAuditMapper;
    
    @Autowired
    private UserMapper userMapper;

    @Override
    public String getToken() {
        return "knowledge/document/audit/list";
    }

    @Override
    public String getName() {
        return "知识库文档活动列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
        @Param(name = "knowledgeDocumentId", type = ApiParamType.LONG, isRequired = true, desc = "文档id"),
        @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true"),
        @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页条目"),
        @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页")
    })
    @Output({
        @Param(explode = BasePageVo.class),
        @Param(name = "list", explode = KnowledgeDocumentAuditVo[].class, desc = "文档版本列表")
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        resultObj.put("list", new ArrayList<>());
        KnowledgeDocumentAuditVo searchVo = JSON.toJavaObject(jsonObj, KnowledgeDocumentAuditVo.class);
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(searchVo.getKnowledgeDocumentId());
        if(knowledgeDocumentVo == null) {
            throw new KnowledgeDocumentNotFoundException(searchVo.getKnowledgeDocumentId());
        }
        int pageCount = 0;
        if(searchVo.getNeedPage()) {
            int rowNum = knowledgeDocumentAuditMapper.getKnowledgeDocumentAuditCountByKnowledgeDocumentId(searchVo);
            pageCount = PageUtil.getPageCount(rowNum, searchVo.getPageSize());
            resultObj.put("currentPage", searchVo.getCurrentPage());
            resultObj.put("pageSize", searchVo.getPageSize());
            resultObj.put("pageCount", pageCount);
            resultObj.put("rowNum", rowNum);
        }
        if(!searchVo.getNeedPage() || searchVo.getCurrentPage() <= pageCount) {
            UserVo currentUserVo = userMapper.getUserBaseInfoByUuid(UserContext.get().getUserUuid(true));
            List<KnowledgeDocumentAuditVo> knowledgeDocumentAuditList = knowledgeDocumentAuditMapper.getKnowledgeDocumentAuditListByKnowledgeDocumentId(searchVo);
            for(KnowledgeDocumentAuditVo knowledgeDocumentVersionVo : knowledgeDocumentAuditList) {
                knowledgeDocumentVersionVo.setLcuName(currentUserVo.getUserName());
                knowledgeDocumentVersionVo.setLcuInfo(currentUserVo.getUserInfo());
            }
            resultObj.put("list", knowledgeDocumentAuditList);
        }
        return resultObj;
    }

}
