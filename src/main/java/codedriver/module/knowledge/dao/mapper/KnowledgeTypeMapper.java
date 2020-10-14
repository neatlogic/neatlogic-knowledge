package codedriver.module.knowledge.dao.mapper;

import codedriver.module.knowledge.dto.KnowledgeTypeVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KnowledgeTypeMapper {
    public Integer getMaxRhtCode();

    public List<KnowledgeTypeVo> getKnowledgeTypeByParentId(Long parentId);

    public void updateKnowledgeTypeLeftRightCode(@Param("id") Long id, @Param("lft") Integer lft, @Param("rht") Integer rht);

    public void batchInsertKnowledgeType(@Param("list") List<KnowledgeTypeVo> list);

    public void deleteKnowledgeTypeByCircleId(Long knowledgeCircleId);

}
