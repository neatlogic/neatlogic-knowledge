package codedriver.module.knowledge.api.circle;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeCircleMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import codedriver.module.knowledge.exception.KnowledgeCircleHasKnowledgeException;
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
	private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;

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

	@Input({@Param( name = "id", type = ApiParamType.LONG, desc = "知识圈ID",isRequired = true)})
	@Output({})
	@Description(desc = "删除知识圈")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {

		Long id = jsonObj.getLong("id");
		if(knowledgeCircleMapper.checkKnowledgeCircleExistsById(id) == 0){
			throw new KnowledgeCircleNotFoundException(id);
		}
		if(knowledgeCircleMapper.checkCircleHasKnowledge(id) > 0){
			throw new KnowledgeCircleHasKnowledgeException(id);
		}
		/** 删除知识圈用户、知识类型 */
		knowledgeCircleMapper.deleteKnowledgeCircleUserById(id);
		knowledgeDocumentTypeMapper.deleteTypeByCircleId(id);
		knowledgeCircleMapper.deleteKnowledgeCircleById(id);
		return null;
	}

}
