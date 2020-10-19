package codedriver.module.knowledge.dto;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;
import com.alibaba.fastjson.annotation.JSONField;

import java.util.ArrayList;
import java.util.List;

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
	@EntityField(name = "关联的知识数", type = ApiParamType.INTEGER)
	private Integer knowledgeCount;

	private transient KnowledgeTypeVo parent;

	private List<KnowledgeTypeVo> children = new ArrayList<>();

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

	public Integer getKnowledgeCount() {
		return knowledgeCount;
	}

	public void setKnowledgeCount(Integer knowledgeCount) {
		this.knowledgeCount = knowledgeCount;
	}

	public KnowledgeTypeVo getParent() {
		return parent;
	}

	public void setParent(KnowledgeTypeVo parent) {
		this.parent = parent;
		parent.getChildren().add(this);
	}

	public List<KnowledgeTypeVo> getChildren() {
		return children;
	}

	public void setChildren(List<KnowledgeTypeVo> children) {
		this.children = children;
	}
}
