package neatlogic.module.knowledge.api.document;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVo;
@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
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
