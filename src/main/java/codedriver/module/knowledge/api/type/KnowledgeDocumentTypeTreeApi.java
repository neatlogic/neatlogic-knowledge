package codedriver.module.knowledge.api.type;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentTypeVo;
import codedriver.module.knowledge.exception.KnowledgeDocumentTypeNotFoundException;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeDocumentTypeTreeApi extends PrivateApiComponentBase{

	@Autowired
	private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;

	@Override
	public String getToken() {
		return "knowledge/document/type/tree";
	}

	@Override
	public String getName() {
		return "获取知识圈知识分类树";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
			@Param( name = "parentUuid", desc = "父级uuid", type = ApiParamType.STRING),
			@Param( name = "knowledgeCircleId", desc = "知识圈ID", type = ApiParamType.LONG ,isRequired = true)
	})
	@Output({
			@Param( name = "typeList", explode = KnowledgeDocumentTypeVo[].class, desc = "知识类型架构集合")
	})
	@Description(desc = "获取知识圈知识分类树")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		JSONObject result = new JSONObject();
		KnowledgeDocumentTypeVo type = new KnowledgeDocumentTypeVo();
		String parentUuid = jsonObj.getString("parentUuid");
		Long knowledgeCircleId = jsonObj.getLong("knowledgeCircleId");
		if (StringUtils.isNotBlank(parentUuid)){
			if(knowledgeDocumentTypeMapper.checkTypeIsExists(parentUuid) == 0) {
				throw new KnowledgeDocumentTypeNotFoundException(parentUuid);
			}
		}else {
			parentUuid = KnowledgeDocumentTypeVo.ROOT_UUID;
		}
		type.setParentUuid(parentUuid);
		type.setKnowledgeCircleId(knowledgeCircleId);
		List<KnowledgeDocumentTypeVo> typeList = knowledgeDocumentTypeMapper.searchType(type);
		result.put("typeList",typeList);
		return result;
	}

}
