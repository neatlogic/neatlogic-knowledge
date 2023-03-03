package neatlogic.module.knowledge.api.template;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeTemplateMapper;
import neatlogic.framework.knowledge.dto.KnowledgeTemplateVo;
import neatlogic.framework.knowledge.exception.KnowledgeTemplateNameRepeatException;
import neatlogic.framework.knowledge.exception.KnowledgeTemplateNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_TEMPLATE_MODIFY;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@AuthAction(action = KNOWLEDGE_TEMPLATE_MODIFY.class)
@Service
@OperationType(type = OperationTypeEnum.CREATE)
public class KnowledgeTemplateSaveApi extends PrivateApiComponentBase{

	@Autowired
	private KnowledgeTemplateMapper knowledgeTemplateMapper;

	@Override
	public String getToken() {
		return "knowledge/template/save";
	}

	@Override
	public String getName() {
		return "保存知识模版";
	}

	@Override
	public String getConfig() {
		return null;
	}


	@Input({
			@Param( name = "id", type = ApiParamType.LONG, desc = "模版ID"),
			@Param( name = "name", type = ApiParamType.REGEX, rule = RegexUtils.NAME, desc = "模版名称", isRequired = true,xss=true),
			@Param( name = "content", type = ApiParamType.JSONARRAY, desc = "目录", isRequired = true)
	})
	@Output({@Param(name = "id", type = ApiParamType.INTEGER,desc = "模版ID")})
	@Description(desc = "保存知识模版")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		Long id = jsonObj.getLong("id");
		String name = jsonObj.getString("name");
		String content = jsonObj.getString("content");
		KnowledgeTemplateVo knowledgeTemplateVo = new KnowledgeTemplateVo();
		knowledgeTemplateVo.setName(name);
		knowledgeTemplateVo.setContent(content);
		knowledgeTemplateVo.setIsActive(1);
		knowledgeTemplateVo.setLcu(UserContext.get().getUserUuid());

		if(id != null){
			if(knowledgeTemplateMapper.checkKnowledgeTemplateExistsById(id) == 0){
				throw new KnowledgeTemplateNotFoundException(id);
			}
			knowledgeTemplateVo.setId(id);
			if(knowledgeTemplateMapper.checkNameIsRepeat(knowledgeTemplateVo) > 0){
				throw new KnowledgeTemplateNameRepeatException(name);
			}
			knowledgeTemplateMapper.updateKnowledgeTemplate(knowledgeTemplateVo);
		}else{
			if(knowledgeTemplateMapper.checkNameIsRepeat(knowledgeTemplateVo) > 0){
				throw new KnowledgeTemplateNameRepeatException(name);
			}
			knowledgeTemplateVo.setFcu(UserContext.get().getUserUuid());
			knowledgeTemplateMapper.insertKnowledgeTemplate(knowledgeTemplateVo);
		}
		JSONObject result = new JSONObject();
		result.put("id",knowledgeTemplateVo.getId());
		return result;
	}

	public IValid name(){
		return value -> {
			KnowledgeTemplateVo knowledgeTemplateVo = JSON.toJavaObject(value, KnowledgeTemplateVo.class);
			if(knowledgeTemplateMapper.checkNameIsRepeat(knowledgeTemplateVo) > 0) {
				return new FieldValidResultVo(new KnowledgeTemplateNameRepeatException(knowledgeTemplateVo.getName()));
			}
			return new FieldValidResultVo();
		};
	}

}
