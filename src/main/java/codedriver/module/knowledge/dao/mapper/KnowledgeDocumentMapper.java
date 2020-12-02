package codedriver.module.knowledge.dao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.elasticsearch.annotation.ESParam;
import codedriver.framework.elasticsearch.annotation.ESSearch;
import codedriver.module.knowledge.dto.KnowledgeDocumentCollectVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentFileVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentHistoricalVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentInvokeVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineConfigVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineContentVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentTagVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;

public interface KnowledgeDocumentMapper {

    public KnowledgeDocumentVo getKnowledgeDocumentById(Long id);

    public KnowledgeDocumentVo getKnowledgeDocumentByTitle(String title);
    
    public List<KnowledgeDocumentVo> getKnowledgeDocumentByIdList(@Param("documentIdlist") List<Long> documentIdList);
    
    public List<KnowledgeDocumentVersionVo> getKnowledgeDocumentVersionByIdList(@Param("documentVersionIdlist") List<Long> documentVersionIdList);
    
    public List<Long> getKnowledgeDocumentIdList(KnowledgeDocumentVo knowledgeDocumentVo);
    
    public List<Long> getKnowledgeDocumentVersionIdList(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);
    
    public Integer getKnowledgeDocumentVersionCount(KnowledgeDocumentVersionVo KnowledgeDocumentVersionVo);
    
    public Integer getKnowledgeDocumentCount(KnowledgeDocumentVo knowledgeDocumentVo);
    
    public List<KnowledgeDocumentVo> getKnowledgeDocumentByIdListAndFcd(@Param("documentIdlist") List<Long> documentIdList,@Param("fromDate") String fromDate,@Param("toDate")String toDate);
    
    public List<Long> getKnowledgeDocumentVersionIdListByLcd(@Param("documentVersionIdlist") List<Long> documentVersionIdList,@Param("fromDate") String fromDate,@Param("toDate")String toDate);
    
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

    public List<KnowledgeDocumentVersionVo> getKnowledgeDocumentWaitingForReviewList(
        @Param("basePageVo") BasePageVo basePageVo,
        @Param("userUuid") String userUuid, 
        @Param("teamUuidList") List<String> teamUuidList, 
        @Param("roleUuidList") List<String> roleUuidList
        );

    public int getKnowledgeDocumentWaitingForReviewCount(
        @Param("basePageVo") BasePageVo basePageVo,
        @Param("userUuid") String userUuid, 
        @Param("teamUuidList") List<String> teamUuidList, 
        @Param("roleUuidList") List<String> roleUuidList
        );

    public List<KnowledgeDocumentHistoricalVersionVo> getKnowledgeDocumentHistorialVersionListByKnowledgeDocumentId(Long knowledgeDocumentId);
    
    public int getKnowledgeDocumentHistorialVersionCountByKnowledgeDocumentId(Long knowledgeDocumentId);
    
    public List<ValueTextVo> getKnowledgeDocumentHistorialVersionListForSelectByKnowledgeDocumentId(Long knowledgeDocumentId);

    public List<ValueTextVo> getKnowledgeDocumentListForInternalLink(BasePageVo basePageVo);
    
    public int getKnowledgeDocumentCountForInternalLink(BasePageVo basePageVo);

    public int getCurrentUserKnowledgeDocumentCount(
        @Param("userUuid")String userUuid, 
        @Param("teamUuidList")List<String> teamUuidList, 
        @Param("roleUuidList")List<String> roleUuidList
    );

    public int checkUserIsApprover(
        @Param("knowledgeCircleId") Long knowledgeCircleId, 
        @Param("userUuid") String userUuid, 
        @Param("teamUuidList") List<String> teamUuidList, 
        @Param("roleUuidList") List<String> roleUuidList
        );
    
    public List<Long> getUserAllApproverCircleIdList(
        @Param("userUuid") String userUuid, 
        @Param("teamUuidList") List<String> teamUuidList, 
        @Param("roleUuidList") List<String> roleUuidList
        );
    
    public int checkUserIsMember(
        @Param("knowledgeCircleId") Long knowledgeCircleId, 
        @Param("userUuid") String userUuid, 
        @Param("teamUuidList") List<String> teamUuidList, 
        @Param("roleUuidList") List<String> roleUuidList 
        );

    public List<KnowledgeDocumentVersionVo> getKnowledgeDocumentListByKnowledgeDocumentTypeUuid(KnowledgeDocumentVo knowledgeDocumentVo);

    public int getKnowledgeDocumentCountByKnowledgeDocumentTypeUuid(KnowledgeDocumentVo knowledgeDocumentVo);

    public int checkDocumentHasBeenFavored(@Param("documentId") Long documentId,@Param("userUuid") String userUuid);

    public int getDocumentFavorCount(Long documentId);

    public int checkDocumentHasBeenCollected(@Param("documentId") Long documentId,@Param("userUuid") String userUuid);

    public int getDocumentCollectCount(Long documentId);

    public int checkExistsDocumentViewCount(Long documentId);

    public int getDocumentViewCount(Long documentId);

//    public int checkIFThereIsSubmittedDraftByKnowDocumentIdAndFromVersion(@Param("knowledgeDocumentId")Long knowledgeDocumentId, @Param("fromVersion")Integer fromVersion);
    
    public int checkIFThereIsSubmittedDraftByKnowDocumentId(Long knowledgeDocumentId);

    public int checkIfTheVersionIsTheCurrentVersion(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);

    public List<KnowledgeDocumentVersionVo> getKnowledgeDocumentVersionMyCollectList(KnowledgeDocumentCollectVo knowledgeDocumentCollectVo);

    public int getKnowledgeDocumentVersionMyCollectCount(KnowledgeDocumentCollectVo knowledgeDocumentCollectVo);

    public List<Long> getKnowledgeDocumentCollectDocumentIdListByUserUuidAndDocumentIdList(@Param("userUuid") String userUuid, @Param("knowledgeDocumentIdList") List<Long> knowledgeDocumentIdList);
    
    public List<Long> getKnowledgeDocumentFavorDocumentIdListByUserUuidAndDocumentIdList(@Param("userUuid") String userUuid, @Param("knowledgeDocumentIdList") List<Long> knowledgeDocumentIdList);

    public Long getKnowledgeDocumentDrafIdtByKnowledgeDocumentIdAndLcu(@Param("knowledgeDocumentId")Long knowledgeDocumentId, @Param("lcu")String userUuid);

    public Long getKnowledgeDocumentIdByInvokeIdAndSource(KnowledgeDocumentInvokeVo knowledgeDocumentInvokeVo);

    public KnowledgeDocumentVersionVo getKnowledgeDocumentVersionByknowledgeDocumentIdLimitOne(Long knowledgeDocumentId);

    @ESSearch
    public int insertKnowledgeDocument(@ESParam("knowledge")KnowledgeDocumentVo knowledgeDocumentVo);

    public int insertKnowledgeDocumentVersion(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);

    public int insertKnowledgeDocumentFile(KnowledgeDocumentFileVo knowledgeDocumentFileVo);

    public int insertKnowledgeDocumentTag(KnowledgeDocumentTagVo knowledgeDocumentTagVo);

    public int insertKnowledgeDocumentLineConfig(KnowledgeDocumentLineConfigVo knowledgeDocumentLineConfigVo);
    
    public int insertKnowledgeDocumentLineContent(KnowledgeDocumentLineContentVo knowledgeDocumentLineContentVo);

    @ESSearch
    public int insertKnowledgeDocumentLineList(@ESParam("knowledgeversion")List<KnowledgeDocumentLineVo> knowledgeDocumentLineList);

    public int insertKnowledgeDocumentFavor(@Param("documentId") Long documentId,@Param("userUuid") String userUuid);

    public int insertKnowledgeDocumentCollect(@Param("documentId") Long documentId,@Param("userUuid") String userUuid);

    public int insertKnowledgeDocumentViewCount(@Param("documentId") Long documentId,@Param("count") int count);

    public int insertKnowledgeDocumentInvoke(KnowledgeDocumentInvokeVo knowledgeDocumentInvokeVo);

    public int updateKnowledgeDocumentToDeleteById(Long knowledgeDocumentId);

    public int updateKnowledgeDocumentVersionById(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);

    public int updateKnowledgeDocumentVersionStatusByKnowledgeDocumentIdAndVersionAndStatus(
        @Param("knowledgeDocumentId") Long knowledgeDocumentId, 
        @Param("fromVersion") Integer fromVersion, 
        @Param("oldStatus") String oldStatus, 
        @Param("newStatus") String newStatus
    );

    @ESSearch
    public int updateKnowledgeDocumentById(@ESParam("knowledge")KnowledgeDocumentVo knowledgeDocumentVo);

    public int updateKnowledgeViewCountIncrementOne(Long documentId);

    public int updateKnowledgeDocumentVersionToDeleteByKnowledgeDocumentId(Long knowledgeDocumentId);

    public int updateKnowledgeDocumentVersionToDeleteById(Long id);

    public int deleteKnowledgeDocumentLineByKnowledgeDocumentVersionId(Long knowledgeDocumentVersionId);

    public int deleteKnowledgeDocumentFileByKnowledgeDocumentIdAndVersionId(KnowledgeDocumentFileVo knowledgeDocumentFileVo);

    public int deleteKnowledgeDocumentTagByKnowledgeDocumentIdAndVersionId(KnowledgeDocumentTagVo knowledgeDocumentTagVo);

    public int deleteKnowledgeDocumentVersionById(Long id);

    public int deleteKnowledgeDocumentById(Long id);

    public int deleteKnowledgeDocumentFavor(@Param("documentId") Long documentId,@Param("userUuid") String userUuid);

    public int deleteKnowledgeDocumentCollect(@Param("documentId") Long documentId,@Param("userUuid") String userUuid);

    public int deleteKnowledgeDocumentDraftByKnowledgeDocumentIdAndLcu(@Param("knowledgeDocumentId")Long knowledgeDocumentId, @Param("lcu")String userUuid);

    public int deleteKnowledgeDocumentInvokeByKnowledgeDocumentId(Long knowledgeDocumentId);

}
