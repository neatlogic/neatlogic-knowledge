package codedriver.module.knowledge.api.type;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentTypeVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentTypeNotFoundException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentTypeSubTreeApi extends PrivateApiComponentBase{

	@Autowired
	private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;

	@Override
	public String getToken() {
		return "knowledge/document/type/subtree";
	}

	@Override
	public String getName() {
		return "获取知识圈知识分类树_子树";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({@Param( name = "parentUuid", desc = "父级uuid", type = ApiParamType.STRING,isRequired = true)})
	@Output({@Param( name = "typeList", explode = KnowledgeDocumentTypeVo[].class, desc = "知识类型架构集合")})
	@Description(desc = "获取知识圈知识分类树_子树")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		JSONObject result = new JSONObject();
		KnowledgeDocumentTypeVo type = new KnowledgeDocumentTypeVo();
		String parentUuid = jsonObj.getString("parentUuid");
		if(knowledgeDocumentTypeMapper.checkTypeIsExists(parentUuid) == 0) {
			throw new KnowledgeDocumentTypeNotFoundException(parentUuid);
		}
		type.setParentUuid(parentUuid);
		List<KnowledgeDocumentTypeVo> typeList = knowledgeDocumentTypeMapper.searchType(type);
		result.put("typeList",typeList);
		return result;
	}

}
