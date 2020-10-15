package codedriver.module.knowledge.api;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentNotCurrentVersionException;
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentSaveApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Override
    public String getToken() {
        return "knowledge/document/save";
    }

    @Override
    public String getName() {
        return "保存文档";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({
        @Param(name = "id", type = ApiParamType.LONG, desc = "文档id"),
        @Param(name = "knowledgeDocumentVersionId", type = ApiParamType.LONG, isRequired = true, desc = "版本id"),
        @Param(name = "knowledgeTypeId", type = ApiParamType.LONG, isRequired = true, desc = "类型id"),
        @Param(name = "knowledgeCircleId", type = ApiParamType.LONG, isRequired = true, desc = "知识圈id"),
        @Param(name = "title", type = ApiParamType.LONG, isRequired = true, desc = "标题"),
        @Param(name = "lineList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "行数据列表"),
        @Param(name = "fileIdList", type = ApiParamType.JSONARRAY, desc = "附件id列表"),
        @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, desc = "标签id列表")
    })
    @Output({
        @Param(name = "id", type = ApiParamType.LONG, desc = "文档id")
    })
    @Description(desc = "保存文档")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
//        KnowledgeDocumentVo documentVo = JSON.toJavaObject(jsonObj, KnowledgeDocumentVo.class);
        KnowledgeDocumentVo oldDocumentVo = knowledgeDocumentMapper.getKnowledgeDocumentById(id);
        if(oldDocumentVo != null) {
            if(Objects.equals(oldDocumentVo.getIsDelete(), 0)) {
                Long knowledgeDocumentVersionId = jsonObj.getLong("knowledgeDocumentVersionId");
                if(!knowledgeDocumentVersionId.equals(oldDocumentVo.getKnowledgeDocumentVersionId())) {
                    throw new KnowledgeDocumentNotCurrentVersionException(knowledgeDocumentVersionId);
                }
            }else {
                
            }
        }
        return null;
    }

}
