package codedriver.module.knowledge.service;

import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;

public interface KnowledgeDocumentService {

    public int isDeletable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);
    
    public int isEditable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);
    
    public int isReviewable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);
    
    public KnowledgeDocumentVo getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(Long knowledgeDocumentVersionId) throws PermissionDeniedException;

    /**
     * @Author 89770
     * @Time 2020年12月3日
     * @Description: 根据不同的审批状态，补充审批人条件
     * @Param
     * @return
     */
    public void getReviewerParam(KnowledgeDocumentVersionVo documentVersionVoParam);
}
