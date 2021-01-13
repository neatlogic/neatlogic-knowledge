package codedriver.module.knowledge.api.document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentTitleCheckApi extends PrivateApiComponentBase {

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Override
    public String getToken() {
        return "knowledge/document/title/check";
    }

    @Override
    public String getName() {
        return "检查知识库文档标题是否存在";
    }

    @Override
    public String getConfig() {
        return null;
    }
    
    @Input({
        @Param(name = "title", type = ApiParamType.STRING, isRequired = true, desc = "文档标题")
    })
    @Output({
        @Param(explode = KnowledgeDocumentVo.class, desc = "文档内容")
    })
    @Description(desc = "检查知识库文档标题是否存在")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String title = jsonObj.getString("title");
        KnowledgeDocumentVo knowledgeDocument = knowledgeDocumentMapper.getKnowledgeDocumentByTitle(title);
        return knowledgeDocument;
    }

}
