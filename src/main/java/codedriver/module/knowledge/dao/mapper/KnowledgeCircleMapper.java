package codedriver.module.knowledge.dao.mapper;

import codedriver.module.knowledge.dto.KnowledgeCircleUserVo;
import codedriver.module.knowledge.dto.KnowledgeCircleVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KnowledgeCircleMapper {

    public int checkKnowledgeCircleExistsById(Long id);

    public int checkNameIsRepeat(KnowledgeCircleVo knowledgeCircleVo);

    public int searchKnowledgeCircleCount(KnowledgeCircleVo knowledgeCircleVo);

    public List<KnowledgeCircleVo> searchKnowledgeCircle(KnowledgeCircleVo knowledgeCircleVo);

    public List<KnowledgeCircleUserVo> getKnowledgeCircleUserList(Long knowledgeCircleId);

    public void updateKnowledgeCircle(KnowledgeCircleVo knowledgeCircleVo);

    public void insertKnowledgeCircle(KnowledgeCircleVo knowledgeCircleVo);

    public void batchInsertKnowledgeCircleUser(@Param("list") List<KnowledgeCircleUserVo> list);

    public void deleteKnowledgeCircleById(Long id);

    public void deleteKnowledgeCircleUserById(Long circleId);
}
