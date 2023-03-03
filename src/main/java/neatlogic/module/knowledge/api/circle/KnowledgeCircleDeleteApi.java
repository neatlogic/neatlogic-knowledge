package neatlogic.module.knowledge.api.circle;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_CIRCLE_MODIFY;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeCircleMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import neatlogic.framework.knowledge.exception.KnowledgeCircleHasKnowledgeException;
import neatlogic.framework.knowledge.exception.KnowledgeCircleNotFoundException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AuthAction(action = KNOWLEDGE_CIRCLE_MODIFY.class)
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
