package neatlogic.module.knowledge.api.template;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.knowledge.exception.KnowledgeTemplateNotFoundEditTargetException;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeTemplateMapper;
import neatlogic.framework.knowledge.dto.KnowledgeTemplateVo;
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
		return "nmkat.knowledgetemplategetapi.getname";
	}

	@Override
	public String getConfig() {
		return null;
	}


	@Input({
			@Param( name = "id", type = ApiParamType.LONG, desc = "common.id",isRequired = true)
	})
	@Output({@Param(name = "template",explode = KnowledgeTemplateVo.class,desc = "term.knowledge.templateinfo")})
	@Description(desc = "nmkat.knowledgetemplategetapi.getname")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		Long id = jsonObj.getLong("id");
		KnowledgeTemplateVo knowledgeTemplateVo = knowledgeTemplateMapper.getKnowledgeTemplateById(id);
		if(knowledgeTemplateVo == null){
			throw new KnowledgeTemplateNotFoundEditTargetException(id);
		}
		JSONObject result = new JSONObject();
		result.put("template", knowledgeTemplateVo);
		return result;
	}

}
