package neatlogic.module.knowledge.api.template;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_TEMPLATE_MODIFY;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeTemplateMapper;
import neatlogic.framework.knowledge.exception.KnowledgeTemplateNotFoundException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@AuthAction(action = KNOWLEDGE_TEMPLATE_MODIFY.class)
@Service
@OperationType(type = OperationTypeEnum.DELETE)
public class KnowledgeTemplateDeleteApi extends PrivateApiComponentBase{

	@Autowired
	private KnowledgeTemplateMapper knowledgeTemplateMapper;

	@Override
	public String getToken() {
		return "knowledge/template/delete";
	}

	@Override
	public String getName() {
		return "删除知识模版";
	}

	@Override
	public String getConfig() {
		return null;
	}


	@Input({
			@Param( name = "id", type = ApiParamType.LONG, desc = "模版ID",isRequired = true)
	})
	@Output({})
	@Description(desc = "删除知识模版")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		Long id = jsonObj.getLong("id");
		if(knowledgeTemplateMapper.checkKnowledgeTemplateExistsById(id) == 0){
			throw new KnowledgeTemplateNotFoundException(id);
		}
		knowledgeTemplateMapper.deleteKnowledgeTemplate(id);
		return null;
	}

}
