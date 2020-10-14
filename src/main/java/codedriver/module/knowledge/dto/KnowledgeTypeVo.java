package codedriver.module.knowledge.dto;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;
import com.alibaba.fastjson.annotation.JSONField;

public class KnowledgeTypeVo{

	public static final Long ROOT_PARENTID = -1L;
	public static final Long ROOT_ID = 0L;

	@EntityField(name = "知识类型ID", type = ApiParamType.LONG)
	private Long id;
	@EntityField(name = "知识类型名称", type = ApiParamType.STRING)
	private String name;
	@EntityField(name = "知识圈ID", type = ApiParamType.LONG)
	private Long knowledgeCircleId;
	@EntityField(name = "父类型ID", type = ApiParamType.LONG)
	private Long parentId;
	@JSONField(serialize = false)
	@EntityField(name = "左编码", type = ApiParamType.INTEGER)
	private transient Integer lft;
	@JSONField(serialize = false)
	@EntityField(name = "右编码", type = ApiParamType.INTEGER)
	private transient Integer rht;
	@JSONField(serialize = false)
	@EntityField(name = "排序（相对于同级节点的顺序）", type = ApiParamType.INTEGER)
	private transient Integer sort;
	@EntityField(name = "子节点数量", type = ApiParamType.INTEGER)
	private Integer childCount;

	public KnowledgeTypeVo() {}

	public Long getId() {
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

	public Long getKnowledgeCircleId() {
		return knowledgeCircleId;
	}

	public void setKnowledgeCircleId(Long knowledgeCircleId) {
		this.knowledgeCircleId = knowledgeCircleId;
	}

	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	public Integer getLft() {
		return lft;
	}

	public void setLft(Integer lft) {
		this.lft = lft;
	}

	public Integer getRht() {
		return rht;
	}

	public void setRht(Integer rht) {
		this.rht = rht;
	}

	public Integer getSort() {
		return sort;
	}

	public void setSort(Integer sort) {
		this.sort = sort;
	}

	public Integer getChildCount() {
		return childCount;
	}

	public void setChildCount(Integer childCount) {
		this.childCount = childCount;
	}
}
