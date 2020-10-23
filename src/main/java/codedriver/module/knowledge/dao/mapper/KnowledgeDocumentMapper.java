package codedriver.module.knowledge.dao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.elasticsearch.annotation.ESParam;
import codedriver.framework.elasticsearch.annotation.ESSearch;
import codedriver.module.knowledge.dto.KnowledgeDocumentFileVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentHistoricalVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineConfigVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineContentVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentTagVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;

public interface KnowledgeDocumentMapper {

    public KnowledgeDocumentVo getKnowledgeDocumentById(Long id);

    public KnowledgeDocumentVersionVo getKnowledgeDocumentVersionById(Long id);
    
    public int checkKnowledgeDocumentLineConfigHashIsExists(String hash);

    public int checkKnowledgeDocumentLineContentHashIsExists(String hash);

    public Long getKnowledgeDocumentLockById(Long id);

    public List<KnowledgeDocumentVersionVo> getKnowledgeDocumentVersionMyVersionList(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);

    public int getKnowledgeDocumentVersionMyVersionCount(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);

    public List<KnowledgeDocumentLineVo> getKnowledgeDocumentLineListByKnowledgeDocumentVersionId(Long knowledgeDocumentVersionId);

    public List<Long> getKnowledgeDocumentFileIdListByKnowledgeDocumentIdAndVersionId(KnowledgeDocumentFileVo knowledgeDocumentFileVo);

    public List<Long> getKnowledgeDocumentTagIdListByKnowledgeDocumentIdAndVersionId(KnowledgeDocumentTagVo knowledgeDocumentTagVo);

    public Integer getKnowledgeDocumentVersionMaxVerionByKnowledgeDocumentId(Long knowledgeDocumentId);

    public List<KnowledgeDocumentVersionVo> getKnowledgeDocumentWaitingForReviewList(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);

    public int getKnowledgeDocumentWaitingForReviewCount(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);

    public List<KnowledgeDocumentHistoricalVersionVo> getKnowledgeDocumentHistorialVersionListByKnowledgeDocumentId(Long knowledgeDocumentId);

    public List<ValueTextVo> getKnowledgeDocumentListForInternalLink(BasePageVo basePageVo);
    
    public int getKnowledgeDocumentCountForInternalLink(BasePageVo basePageVo);

    public int getCurrentUserKnowledgeDocumentCount(
        @Param("userUuid")String userUuid, 
        @Param("teamUuidList")List<String> teamUuidList, 
        @Param("roleUuidList")List<String> roleUuidList
    );

    public int checkUserIsApprover(@Param("uuid") String userUuid, @Param("knowledgeCircleId") Long knowledgeCircleId);

    public List<KnowledgeDocumentVersionVo> getKnowledgeDocumentListByKnowledgeDocumentTypeUuid(KnowledgeDocumentVo knowledgeDocumentVo);

    public int getKnowledgeDocumentCountByKnowledgeDocumentTypeUuid(KnowledgeDocumentVo knowledgeDocumentVo);

    public int checkDocumentHasBeenFavored(@Param("documentId") Long documentId,@Param("userUuid") String userUuid);

    public int getDocumentFavorCount(Long documentId);

    public int checkDocumentHasBeenCollected(@Param("documentId") Long documentId,@Param("userUuid") String userUuid);

    public int getDocumentCollectCount(Long documentId);

    public int checkExistsDocumentViewCount(Long documentId);

    public int getDocumentViewCount(Long documentId);

    public int checkIFThereIsSubmittedDraftByKnowDocumentIdAndVersion(@Param("knowledgeDocumentId")Long knowledgeDocumentId, @Param("version")Integer version);

    @ESSearch
    public int insertKnowledgeDocument(@ESParam("knowledge")KnowledgeDocumentVo knowledgeDocumentVo);

    public int insertKnowledgeDocumentVersion(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);

    public int insertKnowledgeDocumentFile(KnowledgeDocumentFileVo knowledgeDocumentFileVo);

    public int insertKnowledgeDocumentTag(KnowledgeDocumentTagVo knowledgeDocumentTagVo);

    public int insertKnowledgeDocumentLineConfig(KnowledgeDocumentLineConfigVo knowledgeDocumentLineConfigVo);

    public int insertKnowledgeDocumentLineContent(KnowledgeDocumentLineContentVo knowledgeDocumentLineContentVo);

    public int insertKnowledgeDocumentLineList(List<KnowledgeDocumentLineVo> knowledgeDocumentLineList);

    public int insertKnowledgeDocumentFavor(@Param("documentId") Long documentId,@Param("userUuid") String userUuid);

    public int insertKnowledgeDocumentCollect(@Param("documentId") Long documentId,@Param("userUuid") String userUuid);

    public int insertKnowledgeDocumentViewCount(@Param("documentId") Long documentId,@Param("count") int count);

    public int updateKnowledgeDocumentToDeleteById(Long knowledgeDocumentId);

    public int updateKnowledgeDocumentVersionById(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);

    public int updateKnowledgeDocumentVersionStatusByKnowledgeDocumentIdAndVersionAndStatus(
        @Param("knowledgeDocumentId") Long knowledgeDocumentId, 
        @Param("version") Integer version, 
        @Param("oldStatus") String oldStatus, 
        @Param("newStatus") String newStatus
    );

    @ESSearch
    public int updateKnowledgeDocumentById(@ESParam("knowledge")KnowledgeDocumentVo knowledgeDocumentVo);

    public int updateKnowledgeViewCountIncrementOne(Long documentId);

    public int deleteKnowledgeDocumentLineByKnowledgeDocumentVersionId(Long knowledgeDocumentVersionId);

    public int deleteKnowledgeDocumentFileByKnowledgeDocumentIdAndVersionId(KnowledgeDocumentFileVo knowledgeDocumentFileVo);

    public int deleteKnowledgeDocumentTagByKnowledgeDocumentIdAndVersionId(KnowledgeDocumentTagVo knowledgeDocumentTagVo);

    public int deleteKnowledgeDocumentVersionById(Long id);

    public int deleteKnowledgeDocumentById(Long id);

    public int deleteKnowledgeDocumentFavor(@Param("documentId") Long documentId,@Param("userUuid") String userUuid);

    public int deleteKnowledgeDocumentCollect(@Param("documentId") Long documentId,@Param("userUuid") String userUuid);

}
