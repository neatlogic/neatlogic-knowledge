package codedriver.module.knowledge.dto;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;

import java.util.List;

public class KnowledgeCircleVo extends BasePageVo {

	@EntityField(name = "知识圈ID", type = ApiParamType.LONG)
	private Long id;
	@EntityField(name = "知识圈名称", type = ApiParamType.STRING)
	private String name;
	@EntityField(name = "知识数", type = ApiParamType.INTEGER)
	private Integer knowledgeCount;
	@EntityField(name = "成员数", type = ApiParamType.INTEGER)
	private Integer memberCount;
	@EntityField(name = "审批人用户名列表", type = ApiParamType.JSONARRAY)
	private List approverNameList;

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

	public Integer getKnowledgeCount() {
		return knowledgeCount;
	}

	public void setKnowledgeCount(Integer knowledgeCount) {
		this.knowledgeCount = knowledgeCount;
	}

	public Integer getMemberCount() {
		return memberCount;
	}

	public void setMemberCount(Integer memberCount) {
		this.memberCount = memberCount;
	}

	public List getApproverNameList() {
		return approverNameList;
	}

	public void setApproverNameList(List approverNameList) {
		this.approverNameList = approverNameList;
	}
}
