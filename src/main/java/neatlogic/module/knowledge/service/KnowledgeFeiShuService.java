package neatlogic.module.knowledge.service;

import neatlogic.framework.knowledge.dto.KnowledgeDocumentTypeVo;
import neatlogic.framework.knowledge.dto.feishu.FeiShuAppCredentialsVo;
import neatlogic.framework.knowledge.dto.feishu.FeiShuNodeVo;

import java.util.List;

public interface KnowledgeFeiShuService {

    FeiShuAppCredentialsVo getFeiShuAppCredentials();

    KnowledgeDocumentTypeVo getOrCreateKnowledgeType(String name, String parentUuid, Long knowledgeCircleId);

    List<FeiShuNodeVo> loadWikiNodes(Long spaceId, String parentNodeToken, List<String> path, String tenantAccessToken);

    void saveNodes(List<FeiShuNodeVo> nodes, FeiShuAppCredentialsVo config, KnowledgeDocumentTypeVo knowledgeType, String tenantAccessToken);

    void saveFeiShuDocument(FeiShuAppCredentialsVo appCredentialsVo, FeiShuNodeVo node, String typeUuid, String tenantAccessToken);
}
