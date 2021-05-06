package codedriver.module.knowledge.api.template;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BaseEditorVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.auth.label.KNOWLEDGE_BASE;
import codedriver.module.knowledge.dao.mapper.KnowledgeTemplateMapper;
import codedriver.module.knowledge.dto.KnowledgeTemplateVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@AuthAction(action = KNOWLEDGE_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class KnowledgeTemplateSearchApi extends PrivateApiComponentBase{

	@Autowired
	private KnowledgeTemplateMapper knowledgeTemplateMapper;

	@Override
	public String getToken() {
		return "knowledge/template/search";
	}

	@Override
	public String getName() {
		return "获取知识模版列表";
	}

	@Override
	public String getConfig() {
		return null;
	}


	@Input({
			@Param(name = "isActive",
					type = ApiParamType.INTEGER,
					desc = "是否激活"),
			@Param( name = "keyword",
					type = ApiParamType.STRING,
					desc = "关键词",
					xss = true),
			@Param(name = "currentPage",
					type = ApiParamType.INTEGER,
					desc = "当前页"),
			@Param(name = "pageSize",
					type = ApiParamType.INTEGER,
					desc = "每页数据条目"),
			@Param(name = "needPage",
					type = ApiParamType.BOOLEAN,
					desc = "是否需要分页，默认true")
	})
	@Output({
			@Param(name = "templateList",
			type = ApiParamType.JSONARRAY,
			explode = KnowledgeTemplateVo[].class,
			desc = "模版列表"),
			@Param(explode = BaseEditorVo.class)
	})
	@Description(desc = "获取知识模版列表")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		KnowledgeTemplateVo knowledgeTemplateVo = JSON.parseObject(jsonObj.toJSONString(), new TypeReference<KnowledgeTemplateVo>(){});
		JSONObject returnObj = new JSONObject();
		if(knowledgeTemplateVo.getNeedPage()){
			int rowNum = knowledgeTemplateMapper.searchKnowledgeTemplateCount(knowledgeTemplateVo);
			returnObj.put("pageSize", knowledgeTemplateVo.getPageSize());
			returnObj.put("currentPage", knowledgeTemplateVo.getCurrentPage());
			returnObj.put("rowNum", rowNum);
			returnObj.put("pageCount", PageUtil.getPageCount(rowNum, knowledgeTemplateVo.getPageSize()));
		}
		List<KnowledgeTemplateVo> templateList = knowledgeTemplateMapper.searchKnowledgeTemplate(knowledgeTemplateVo);
		returnObj.put("templateList", templateList);
		return returnObj;
	}

}
