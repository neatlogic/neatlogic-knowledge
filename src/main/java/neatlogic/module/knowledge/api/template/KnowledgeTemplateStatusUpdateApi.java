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
@OperationType(type = OperationTypeEnum.UPDATE)
public class KnowledgeTemplateStatusUpdateApi extends PrivateApiComponentBase{

	@Autowired
	private KnowledgeTemplateMapper knowledgeTemplateMapper;

	@Override
	public String getToken() {
		return "knowledge/template/status/update";
	}

	@Override
	public String getName() {
		return "修改知识模版激活状态";
	}

	@Override
	public String getConfig() {
		return null;
	}


	@Input({
			@Param( name = "id", type = ApiParamType.LONG, desc = "模版ID",isRequired = true),
			@Param( name = "isActive", type = ApiParamType.ENUM, rule = "0,1", desc = "模版ID",isRequired = true)
	})
	@Output({})
	@Description(desc = "修改知识模版激活状态")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		Long id = jsonObj.getLong("id");
		if(knowledgeTemplateMapper.checkKnowledgeTemplateExistsById(id) == 0){
			throw new KnowledgeTemplateNotFoundException(id);
		}
		Integer isActive = jsonObj.getInteger("isActive");
		knowledgeTemplateMapper.updateActiveStatus(id,isActive);
		return null;
	}

}
