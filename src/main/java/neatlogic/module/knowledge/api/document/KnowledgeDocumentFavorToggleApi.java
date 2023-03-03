package neatlogic.module.knowledge.api.document;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.exception.KnowledgeDocumentNotFoundException;
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
