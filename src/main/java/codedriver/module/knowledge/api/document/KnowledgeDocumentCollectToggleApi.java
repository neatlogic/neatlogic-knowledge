package codedriver.module.knowledge.api.document;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
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
        @Param(name = "documentId", type = ApiParamType.LONG, isRequired = true, desc = "文档id"),
        @Param(name = "isCollect", type = ApiParamType.ENUM,rule = "0,1",isRequired = true, desc = "是否收藏(0：否；1：是)"),
    })
    @Output({
            @Param(name = "count", type = ApiParamType.INTEGER, desc = "文档收藏数"),
    })
    @Description(desc = "收藏或取消收藏文档")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long documentId = jsonObj.getLong("documentId");
        int isCollect = jsonObj.getIntValue("isCollect");
        if(knowledgeDocumentMapper.getKnowledgeDocumentById(documentId) == null){
            throw new KnowledgeDocumentNotFoundException(documentId);
        }
        if(isCollect == 1){
            if(knowledgeDocumentMapper.checkDocumentHasBeenCollected(documentId,UserContext.get().getUserUuid()) > 0){
                throw new KnowledgeDocumentHasBeenCollectedException(documentId);
            }
            knowledgeDocumentMapper.insertKnowledgeDocumentCollect(documentId, UserContext.get().getUserUuid());
        }else if(isCollect == 0){
            knowledgeDocumentMapper.deleteKnowledgeDocumentCollect(documentId, UserContext.get().getUserUuid());
        }
        int favorCount = knowledgeDocumentMapper.getDocumentCollectCount(documentId);
        result.put("count",favorCount);
        return result;
    }

}
