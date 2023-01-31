package neatlogic.module.knowledge.api.template;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeTemplateMapper;
import neatlogic.framework.knowledge.dto.KnowledgeTemplateVo;
import neatlogic.framework.knowledge.exception.KnowledgeTemplateNotFoundException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeTemplateGetApi extends PrivateApiComponentBase{

	@Autowired
	private KnowledgeTemplateMapper knowledgeTemplateMapper;

	@Override
	public String getToken() {
		return "knowledge/template/get";
	}

	@Override
	public String getName() {
		return "获取单个知识模版";
	}

	@Override
	public String getConfig() {
		return null;
	}


	@Input({
			@Param( name = "id", type = ApiParamType.LONG, desc = "模版ID",isRequired = true)
	})
	@Output({@Param(name = "template",type = ApiParamType.JSONARRAY,explode = KnowledgeTemplateVo.class,desc = "知识模版")})
	@Description(desc = "获取单个知识模版")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		Long id = jsonObj.getLong("id");
		if(knowledgeTemplateMapper.checkKnowledgeTemplateExistsById(id) == 0){
			throw new KnowledgeTemplateNotFoundException(id);
		}
		JSONObject result = new JSONObject();
		result.put("template",knowledgeTemplateMapper.getKnowledgeTemplateById(id));
		return result;
	}

}