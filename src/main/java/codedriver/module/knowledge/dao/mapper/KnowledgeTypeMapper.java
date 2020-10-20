package codedriver.module.knowledge.dao.mapper;

import codedriver.module.knowledge.dto.KnowledgeTypeVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KnowledgeTypeMapper {
    public Integer getMaxRhtCode(Long knowledgeCircleId);

    public int checkTypeIsExists(String uuid);

    public List<KnowledgeTypeVo> searchKnowledgeType(KnowledgeTypeVo vo);

    public List<KnowledgeTypeVo> getKnowledgeTypeByParentUuid(@Param("parentUuid") String parentUuid,@Param("knowledgeCircleId") Long knowledgeCircleId);

    public List<KnowledgeTypeVo> getKnowledgeTypeForTree(@Param("lft") Integer lft, @Param("rht") Integer rht,@Param("knowledgeCircleId") Long knowledgeCircleId);

    public void updateKnowledgeTypeLeftRightCode(@Param("uuid") String uuid, @Param("lft") Integer lft, @Param("rht") Integer rht);

    public void batchInsertKnowledgeType(@Param("list") List<KnowledgeTypeVo> list);

    public void deleteKnowledgeTypeByCircleId(Long knowledgeCircleId);

}
