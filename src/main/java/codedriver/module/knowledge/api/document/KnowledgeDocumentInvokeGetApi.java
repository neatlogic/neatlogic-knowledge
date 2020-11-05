package codedriver.module.knowledge.api.document;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentInvokeVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import codedriver.module.knowledge.exception.KnowledgeDocumentVersionNotFoundException;
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentInvokeGetApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Autowired
    private TeamMapper teamMapper;

    @Override
    public String getToken() {
        return "knowledge/document/invoke/get";
    }

    @Override
    public String getName() {
        return "根据调用者查询关联文档信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
        @Param(name = "invokeId", type = ApiParamType.LONG, isRequired = true, desc = "调用者id"),
        @Param(name = "source", type = ApiParamType.STRING, isRequired = true, desc = "来源")
    })
    @Output({
        @Param(name = "isTransferKnowledge", type = ApiParamType.INTEGER, desc = "是否有权限"),
        @Param(name = "knowledgeDocumentVersion", explode = KnowledgeDocumentVersionVo.class, desc = "知识文档版本信息")
    })
    @Description(desc = "根据调用者查询关联文档信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        KnowledgeDocumentInvokeVo knowledgeDocumentInvokeVo = JSON.toJavaObject(jsonObj, KnowledgeDocumentInvokeVo.class);
        Long knowledgeDocumentId = knowledgeDocumentMapper.getKnowledgeDocumentIdByInvokeIdAndSource(knowledgeDocumentInvokeVo);
        if(knowledgeDocumentId != null) {
            resultObj.put("isTransferKnowledge", 0);
            KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(knowledgeDocumentId);
            if(knowledgeDocumentVo == null) {
                throw new KnowledgeDocumentNotFoundException(knowledgeDocumentId);
            }
            if(knowledgeDocumentVo.getKnowledgeDocumentVersionId() != null) {
                KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(knowledgeDocumentVo.getKnowledgeDocumentVersionId());
                if(knowledgeDocumentVersionVo == null) {
                    throw new KnowledgeDocumentVersionNotFoundException(knowledgeDocumentVo.getKnowledgeDocumentVersionId());
                }
                knowledgeDocumentVersionVo.setTitle(knowledgeDocumentVo.getTitle());
                resultObj.put("knowledgeDocumentVersion", knowledgeDocumentVersionVo);
            }else {
                KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionByknowledgeDocumentIdLimitOne(knowledgeDocumentId);
                if(knowledgeDocumentVersionVo != null) {
                    knowledgeDocumentVersionVo.setTitle(knowledgeDocumentVo.getTitle());
                    resultObj.put("knowledgeDocumentVersion", knowledgeDocumentVersionVo);
                }
            }
        }else {
            List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(UserContext.get().getUserUuid(true));
            if(knowledgeDocumentMapper.checkUserIsMember(null, UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList()) == 0) {
                resultObj.put("isTransferKnowledge", 0);
            }else {
                resultObj.put("isTransferKnowledge", 1);
            }
        }
        return resultObj;
    }

}
