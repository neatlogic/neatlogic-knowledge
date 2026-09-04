package neatlogic.module.knowledge.api.circle;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.knowledge.exception.KnowledgeCircleNotFoundEditTargetException;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.knowledge.auth.label.KNOWLEDGE_BASE;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeCircleMapper;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentTypeMapper;
import neatlogic.framework.knowledge.dto.KnowledgeCircleUserVo;
import neatlogic.framework.knowledge.dto.KnowledgeCircleVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentTypeVo;
import neatlogic.module.knowledge.service.KnowledgeDocumentTypeService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
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
		return "nmkac.knowledgecirclegetapi.getname";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({@Param( name = "id", type = ApiParamType.LONG, desc = "common.id",isRequired = true)})
	@Output({@Param(name = "knowledgeCircle",type = ApiParamType.JSONOBJECT,explode = KnowledgeCircleVo.class,desc = "term.knowledge.circleinfo")})
	@Description(desc = "nmkac.knowledgecirclegetapi.getname")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		JSONObject result = new JSONObject();
		Long id = jsonObj.getLong("id");
		if(knowledgeCircleMapper.checkKnowledgeCircleExistsById(id) == 0){
			throw new KnowledgeCircleNotFoundEditTargetException(id);
		}

		KnowledgeCircleVo circle = knowledgeCircleMapper.getKnowledgeCircleById(id);
		/** 查询审批人与成员 */
		List<KnowledgeCircleUserVo> circleUserList = knowledgeCircleMapper.getKnowledgeCircleUserList(id);
		circle.setAuthList(circleUserList);
		/** 查询知识类型 */
		KnowledgeDocumentTypeVo root = knowledgeDocumentTypeService.buildTypeTree(id);
		fillDocumentCount(root.getChildren(), id);
		circle.setDocumentTypeList(root.getChildren());
		result.put("knowledgeCircle",circle);
		return result;
	}

	/**
	 * 补充知识圈详情特有的分类子树文档统计，不参与通用树关系组装。
	 */
	private void fillDocumentCount(List<KnowledgeDocumentTypeVo> typeList, Long knowledgeCircleId) {
		for (KnowledgeDocumentTypeVo typeVo : typeList) {
			typeVo.setDocumentCount(knowledgeDocumentTypeMapper.getDocumentCountByLftRht(
					typeVo.getLft(), typeVo.getRht(), knowledgeCircleId));
			fillDocumentCount(typeVo.getChildren(), knowledgeCircleId);
		}
	}

}
