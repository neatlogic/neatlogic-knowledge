package codedriver.module.knowledge.api.circle;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeCircleMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeTypeMapper;
import codedriver.module.knowledge.dto.KnowledgeCircleUserVo;
import codedriver.module.knowledge.dto.KnowledgeCircleVo;
import codedriver.module.knowledge.dto.KnowledgeTypeVo;
import codedriver.module.knowledge.exception.KnowledgeCircleNotFoundException;
import codedriver.module.knowledge.service.KnowledgeTypeService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeCircleGetApi extends PrivateApiComponentBase{

	@Autowired
	private KnowledgeCircleMapper knowledgeCircleMapper;

	@Autowired
	private KnowledgeTypeMapper knowledgeTypeMapper;

	@Autowired
	private KnowledgeTypeService knowledgeTypeService;

	@Override
	public String getToken() {
		return "knowledge/circle/get";
	}

	@Override
	public String getName() {
		return "获取单个知识圈";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
			@Param( name = "id", type = ApiParamType.LONG, desc = "知识圈ID",isRequired = true)
	})
	@Output({@Param(name = "knowledgeCircle",type = ApiParamType.JSONOBJECT,explode = KnowledgeCircleVo.class,desc = "知识圈")})
	@Description(desc = "获取单个知识圈")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		JSONObject result = new JSONObject();
		Long id = jsonObj.getLong("id");
		if(knowledgeCircleMapper.checkKnowledgeCircleExistsById(id) == 0){
			throw new KnowledgeCircleNotFoundException(id);
		}

		KnowledgeCircleVo circle = knowledgeCircleMapper.getKnowledgeCircleById(id);
		/** 查询审批人与成员 */
		List<KnowledgeCircleUserVo> circleUserList = knowledgeCircleMapper.getKnowledgeCircleUserList(id);
		circle.setAuthList(circleUserList);
		/** 查询知识类型 */
		KnowledgeTypeVo root = knowledgeTypeService.buildRootKnowledgeType();
		List<KnowledgeTypeVo> typeList = knowledgeTypeMapper.getKnowledgeTypeForTree(root.getLft(), root.getRht());
		if(CollectionUtils.isNotEmpty(typeList)){
			Map<Long,KnowledgeTypeVo> idMap = new HashMap<>();
			typeList.add(root);
			for(KnowledgeTypeVo vo : typeList){
				idMap.put(vo.getId(),vo);
			}
			for(KnowledgeTypeVo vo : typeList){
				Long parentId = vo.getParentId();
				KnowledgeTypeVo parent = idMap.get(parentId);
				if(parent != null){
					vo.setParent(parent);
				}
			}
		}
		circle.setKnowledgeTypeList(root.getChildren());
		result.put("knowledgeCircle",circle);
		return result;
	}

}
