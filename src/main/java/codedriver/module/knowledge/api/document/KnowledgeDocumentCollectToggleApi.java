package codedriver.module.knowledge.api.document;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.exception.KnowledgeDocumentHasBeenCollectedException;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotFoundException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
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
