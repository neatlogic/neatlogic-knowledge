package codedriver.module.knowledge.api.circle;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeCircleMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeTypeMapper;
import codedriver.module.knowledge.exception.KnowledgeCircleNotFoundException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AuthAction(name = "KNOWLEDGE_CIRCLE_MODIFY")
@Service
@Transactional
@OperationType(type = OperationTypeEnum.DELETE)
public class KnowledgeCircleDeleteApi extends PrivateApiComponentBase{

	@Autowired
	private KnowledgeCircleMapper knowledgeCircleMapper;

	@Autowired
	private KnowledgeTypeMapper knowledgeTypeMapper;

	@Override
	public String getToken() {
		return "knowledge/circle/delete";
	}

	@Override
	public String getName() {
		return "删除知识圈";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
			@Param( name = "id", type = ApiParamType.LONG, desc = "知识圈ID",isRequired = true)
	})
	@Output({})
	@Description(desc = "删除知识圈")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {

		Long id = jsonObj.getLong("id");
		if(knowledgeCircleMapper.checkKnowledgeCircleExistsById(id) == 0){
			throw new KnowledgeCircleNotFoundException(id);
		}
		knowledgeCircleMapper.deleteKnowledgeCircleById(id);
		knowledgeCircleMapper.deleteKnowledgeCircleUserById(id);
		knowledgeTypeMapper.deleteKnowledgeTypeByCircleId(id);
		return null;
	}

}
