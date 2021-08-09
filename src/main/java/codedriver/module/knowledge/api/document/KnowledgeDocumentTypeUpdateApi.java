package codedriver.module.knowledge.api.document;

import codedriver.framework.auth.core.AuthAction;
import codedriver.module.knowledge.auth.label.KNOWLEDGE_BASE;
import codedriver.framework.knowledge.dto.KnowledgeDocumentVersionVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.framework.knowledge.dto.KnowledgeDocumentVo;
import codedriver.framework.knowledge.exception.KnowledgeDocumentNotFoundException;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class KnowledgeDocumentTypeUpdateApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Override
    public String getToken() {
        return "knowledge/document/type/update";
    }

    @Override
    public String getName() {
        return "更新知识库文档类型";
    }

    @Override
    public String getConfig() {
        return null;
    }
    @Input({
        @Param(name = "knowledgeDocumentId", type = ApiParamType.LONG, isRequired = true, desc = "文档id"),
        @Param(name = "knowledgeDocumentTypeUuid", type = ApiParamType.STRING, isRequired = true, minLength = 1, desc = "新类型uuid")
    })
    @Description(desc = "更新知识库文档类型")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long knowledgeDocumentId = jsonObj.getLong("knowledgeDocumentId");
        KnowledgeDocumentVo knowledgeDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentLockById(knowledgeDocumentId);
        if(knowledgeDocumentVo == null) {
            throw new KnowledgeDocumentNotFoundException(knowledgeDocumentId);
        }
        String knowledgeDocumentTypeUuid = jsonObj.getString("knowledgeDocumentTypeUuid");
        if(!knowledgeDocumentVo.getKnowledgeDocumentTypeUuid().equals(knowledgeDocumentTypeUuid)) {
            knowledgeDocumentVo.setKnowledgeDocumentTypeUuid(knowledgeDocumentTypeUuid);
            knowledgeDocumentMapper.updateKnowledgeDocumentTypeUuidById(knowledgeDocumentVo);
            KnowledgeDocumentVersionVo knowledgeDocumentVersionVo = new KnowledgeDocumentVersionVo();
            knowledgeDocumentVersionVo.setKnowledgeDocumentId(knowledgeDocumentId);
            knowledgeDocumentVersionVo.setKnowledgeDocumentTypeUuid(knowledgeDocumentTypeUuid);
            knowledgeDocumentMapper.updateKnowledgeDocumentVersionTypeByKnowledgeDocumentId(knowledgeDocumentVersionVo);
        }
        return null;
    }

}
