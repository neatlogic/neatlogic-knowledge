package codedriver.module.knowledge.api.document;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.restful.core.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentHistoricalVersionListForSelectApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Autowired
    private TeamMapper teamMapper;

    @Override
    public String getToken() {
        return "knowledge/document/historicalversion/list/forselect";
    }

    @Override
    public String getName() {
        return "查询历史版本列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
        @Param(name = "knowledgeDocumentId", type = ApiParamType.LONG, isRequired = true, desc = "文档id")
    })
    @Output({
        @Param(name = "list", explode = ValueTextVo[].class, desc = "文档历史版本列表")
    })
    @Description(desc = "查询历史版本列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        Long knowledgeDocumentId = jsonObj.getLong("knowledgeDocumentId");
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentId);
        if(knowledgeDocumentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentId);
        }
        List<String> teamUuidList= teamMapper.getTeamUuidListByUserUuid(UserContext.get().getUserUuid(true));
        if(knowledgeDocumentMapper.checkUserIsMember(knowledgeDocumentVo.getKnowledgeCircleId(), UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList()) == 0) {
            throw new PermissionDeniedException();
        }
        List<ValueTextVo> list = knowledgeDocumentMapper.getKnowledgeDocumentHistorialVersionListForSelectByKnowledgeDocumentId(knowledgeDocumentId);
        resultObj.put("list", list);        
        return resultObj;
    }

}
