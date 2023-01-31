package neatlogic.module.knowledge.api.document;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.annotation.*;
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
public class KnowledgeDocumentCollectToggleApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;
    
    @Override
    public String getToken() {
        return "knowledge/document/collect/toggle";
    }

    @Override
    public String getName() {
        return "收藏或取消收藏文档";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({
        @Param(name = "documentId", type = ApiParamType.LONG, isRequired = true, desc = "文档id")
    })
    @Output({
            @Param(name = "count", type = ApiParamType.INTEGER, desc = "文档收藏数"),
            @Param(name = "isCollect", type = ApiParamType.ENUM, rule = "0,1", desc = "是否收藏"),
    })
    @Description(desc = "收藏或取消收藏文档")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long documentId = jsonObj.getLong("documentId");
        if(knowledgeDocumentMapper.getKnowledgeDocumentLockById(documentId) == null){
            throw new KnowledgeDocumentNotFoundException(documentId);
        }
        if(knowledgeDocumentMapper.checkDocumentHasBeenCollected(documentId,UserContext.get().getUserUuid()) == 0){
            knowledgeDocumentMapper.insertKnowledgeDocumentCollect(documentId, UserContext.get().getUserUuid());
            result.put("isCollect", 1);
        }else{
            knowledgeDocumentMapper.deleteKnowledgeDocumentCollect(documentId, UserContext.get().getUserUuid());
            result.put("isCollect", 0);
        }
        int collectCount = knowledgeDocumentMapper.getDocumentCollectCount(documentId);
        result.put("count",collectCount);
        return result;
    }

}
