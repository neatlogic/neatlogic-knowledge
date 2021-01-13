package codedriver.module.knowledge.api.template;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.core.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.auth.label.KNOWLEDGE_TEMPLATE_MODIFY;
import codedriver.module.knowledge.dao.mapper.KnowledgeTemplateMapper;
import codedriver.module.knowledge.dto.KnowledgeTemplateVo;
import codedriver.module.knowledge.exception.KnowledgeTemplateNameRepeatException;
import codedriver.module.knowledge.exception.KnowledgeTemplateNotFoundException;
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
			@Param( name = "name", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", desc = "模版名称", isRequired = true,xss=true),
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

}
