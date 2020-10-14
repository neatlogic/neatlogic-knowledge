package codedriver.module.knowledge.dto;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;

public class KnowledgeCircleVo extends BasePageVo {

	@EntityField(name = "知识圈ID", type = ApiParamType.LONG)
	private Long id;
	@EntityField(name = "知识圈名称", type = ApiParamType.STRING)
	private String name;

	public KnowledgeCircleVo() {}

	public Long getId() {
		if(id == null){
			id = SnowflakeUtil.uniqueLong();
		}
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
