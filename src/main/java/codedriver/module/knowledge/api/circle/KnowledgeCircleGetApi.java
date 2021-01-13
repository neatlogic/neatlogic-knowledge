package codedriver.module.knowledge.api.circle;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.core.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dao.mapper.KnowledgeCircleMapper;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import codedriver.module.knowledge.dto.KnowledgeCircleUserVo;
import codedriver.module.knowledge.dto.KnowledgeCircleVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentTypeVo;
import codedriver.module.knowledge.exception.KnowledgeCircleNotFoundException;
import codedriver.module.knowledge.service.KnowledgeDocumentTypeService;
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
	private KnowledgeDocumentTypeMapper knowledgeDocumentTypeMapper;

	@Autowired
	private KnowledgeDocumentTypeService knowledgeDocumentTypeService;

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

	@Input({@Param( name = "id", type = ApiParamType.LONG, desc = "知识圈ID",isRequired = true)})
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
		KnowledgeDocumentTypeVo root = knowledgeDocumentTypeService.buildRootType(id);
		List<KnowledgeDocumentTypeVo> typeList = knowledgeDocumentTypeMapper.getTypeForTree(root.getLft(), root.getRht(),id);
		if(CollectionUtils.isNotEmpty(typeList)){
			Map<String, KnowledgeDocumentTypeVo> idMap = new HashMap<>();
			typeList.add(root);
			for(KnowledgeDocumentTypeVo vo : typeList){
				idMap.put(vo.getUuid(),vo);
				/** 计算当前分类下的知识数(包括子类的) */
				vo.setDocumentCount(knowledgeDocumentTypeMapper.getDocumentCountByLftRht(vo.getLft(),vo.getRht(),id));
			}
			for(KnowledgeDocumentTypeVo vo : typeList){
				String parentUuid = vo.getParentUuid();
				KnowledgeDocumentTypeVo parent = idMap.get(parentUuid);
				if(parent != null){
					vo.setParent(parent);
				}
			}
		}
		circle.setDocumentTypeList(root.getChildren());
		result.put("knowledgeCircle",circle);
		return result;
	}

}
