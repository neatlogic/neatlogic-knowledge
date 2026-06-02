package neatlogic.framework.knowledge.dao.mapper;

import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncAuditVo;
import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncConfigVo;
import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncDocumentVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KnowledgeFeishuSyncMapper {

    int searchConfigCount(KnowledgeFeishuSyncConfigVo vo);

    List<KnowledgeFeishuSyncConfigVo> searchConfig(KnowledgeFeishuSyncConfigVo vo);

    KnowledgeFeishuSyncConfigVo getConfigById(Long id);

    KnowledgeFeishuSyncConfigVo getConfigByName(String name);

    int checkNameIsRepeat(KnowledgeFeishuSyncConfigVo vo);

    int insertConfig(KnowledgeFeishuSyncConfigVo vo);

    int updateConfig(KnowledgeFeishuSyncConfigVo vo);

    int updateConfigStatus(@Param("id") Long id, @Param("isActive") Integer isActive, @Param("lcu") String lcu);

//    int updateConfigLastSync(KnowledgeFeishuSyncConfigVo vo);

    int deleteConfig(Long id);

    int insertAudit(KnowledgeFeishuSyncAuditVo vo);

    int updateAudit(KnowledgeFeishuSyncAuditVo vo);

    KnowledgeFeishuSyncAuditVo getAuditById(Long id);

    int searchAuditCount(KnowledgeFeishuSyncAuditVo vo);

    List<KnowledgeFeishuSyncAuditVo> searchAudit(KnowledgeFeishuSyncAuditVo vo);

    KnowledgeFeishuSyncDocumentVo getSyncDocumentByNodeToken(@Param("configId") Long configId, @Param("nodeToken") String nodeToken);

    KnowledgeFeishuSyncDocumentVo getSyncDocumentByDocumentId(Long knowledgeDocumentId);

    int insertSyncDocument(KnowledgeFeishuSyncDocumentVo vo);

    int updateSyncDocument(KnowledgeFeishuSyncDocumentVo vo);
}
