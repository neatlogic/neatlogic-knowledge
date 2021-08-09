package codedriver.module.knowledge.api.document;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.auth.label.KNOWLEDGE_BASE;
import codedriver.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.framework.knowledge.exception.KnowledgeDocumentNotFoundException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class KnowledgeDocumentFavorToggleApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    
    @Override
    public String getToken() {
        return "knowledge/document/favor/toggle";
    }

    @Override
    public String getName() {
        return "点赞或取消点赞文档";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({
        @Param(name = "documentId", type = ApiParamType.LONG, isRequired = true, desc = "文档id")
    })
    @Output({
            @Param(name = "count", type = ApiParamType.INTEGER, desc = "文档点赞数"),
            @Param(name = "isFavor", type = ApiParamType.ENUM, rule = "0,1", desc = "是否点赞"),
    })
    @Description(desc = "点赞或取消点赞文档")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long documentId = jsonObj.getLong("documentId");
        if(knowledgeDocumentMapper.getKnowledgeDocumentLockById(documentId) == null){
            throw new KnowledgeDocumentNotFoundException(documentId);
        }
        if(knowledgeDocumentMapper.checkDocumentHasBeenFavored(documentId,UserContext.get().getUserUuid()) == 0){
            knowledgeDocumentMapper.insertKnowledgeDocumentFavor(documentId, UserContext.get().getUserUuid());
            result.put("isFavor", 1);
        }else{
            knowledgeDocumentMapper.deleteKnowledgeDocumentFavor(documentId, UserContext.get().getUserUuid());
            result.put("isFavor", 0);
        }
        int favorCount = knowledgeDocumentMapper.getDocumentFavorCount(documentId);
        result.put("count",favorCount);
        return result;
    }

}
