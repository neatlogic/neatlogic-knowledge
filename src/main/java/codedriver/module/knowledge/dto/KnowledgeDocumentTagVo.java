package codedriver.module.knowledge.dto;

public class KnowledgeDocumentTagVo {

    private Long knowledgeDocumentId;
    private Long knowledgeDocumentVersionId;
    private Long tagId;
    public KnowledgeDocumentTagVo() {
        
    }
    public KnowledgeDocumentTagVo(Long knowledgeDocumentId, Long knowledgeDocumentVersionId, Long tagId) {
        this.knowledgeDocumentId = knowledgeDocumentId;
        this.knowledgeDocumentVersionId = knowledgeDocumentVersionId;
        this.tagId = tagId;
    }
    public Long getKnowledgeDocumentId() {
        return knowledgeDocumentId;
    }
    public void setKnowledgeDocumentId(Long knowledgeDocumentId) {
        this.knowledgeDocumentId = knowledgeDocumentId;
    }
    public Long getKnowledgeDocumentVersionId() {
        return knowledgeDocumentVersionId;
    }
    public void setKnowledgeDocumentVersionId(Long knowledgeDocumentVersionId) {
        this.knowledgeDocumentVersionId = knowledgeDocumentVersionId;
    }
    public Long getTagId() {
        return tagId;
    }
    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }
}
