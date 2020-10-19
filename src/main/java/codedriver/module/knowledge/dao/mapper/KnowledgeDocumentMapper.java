package codedriver.module.knowledge.dao.mapper;

import java.util.List;

import codedriver.module.knowledge.dto.KnowledgeDocumentFileVo;
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

    public List<KnowledgeDocumentVersionVo> getKnowledgeDocumentVersionList(KnowledgeDocumentVersionVo oldKnowledgeDocumentVersionVo);

    public List<KnowledgeDocumentLineVo> getKnowledgeDocumentLineListByKnowledgeDocumentVersionId(Long knowledgeDocumentVersionId);

    public List<Long> getKnowledgeDocumentFileIdListByKnowledgeDocumentIdAndVersionId(KnowledgeDocumentFileVo knowledgeDocumentFileVo);

    public List<Long> getKnowledgeDocumentTagIdListByKnowledgeDocumentIdAndVersionId(KnowledgeDocumentTagVo knowledgeDocumentTagVo);

    public int insertKnowledgeDocument(KnowledgeDocumentVo knowledgeDocumentVo);

    public int insertKnowledgeDocumentVersion(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);

    public int insertKnowledgeDocumentFile(KnowledgeDocumentFileVo knowledgeDocumentFileVo);

    public int insertKnowledgeDocumentTag(KnowledgeDocumentTagVo knowledgeDocumentTagVo);

    public int insertKnowledgeDocumentLineConfig(KnowledgeDocumentLineConfigVo knowledgeDocumentLineConfigVo);

    public int insertKnowledgeDocumentLineContent(KnowledgeDocumentLineContentVo knowledgeDocumentLineContentVo);

    public int insertKnowledgeDocumentLine(KnowledgeDocumentLineVo knowledgeDocumentLineVo);

    public int updateKnowledgeDocumentToDeleteById(Long knowledgeDocumentId);

    public int updateKnowledgeDocumentVersionById(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);

    public int deleteKnowledgeDocumentLineByKnowledgeDocumentVersionId(Long knowledgeDocumentVersionId);

    public int deleteKnowledgeDocumentFileByKnowledgeDocumentIdAndVersionId(KnowledgeDocumentFileVo knowledgeDocumentFileVo);

    public int deleteKnowledgeDocumentTagByKnowledgeDocumentIdAndVersionId(KnowledgeDocumentTagVo knowledgeDocumentTagVo);

    public int deleteKnowledgeDocumentVersionById(Long id);

    public int deleteKnowledgeDocumentById(Long id);

}
