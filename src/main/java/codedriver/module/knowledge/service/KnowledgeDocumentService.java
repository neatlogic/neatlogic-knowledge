package codedriver.module.knowledge.service;

import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentOperate;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import com.alibaba.fastjson.JSONObject;

public interface KnowledgeDocumentService {

    public int isDeletable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);
    
    public int isEditable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);
    
    public int isReviewer(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);
    
    public KnowledgeDocumentVo getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(Long knowledgeDocumentVersionId) throws PermissionDeniedException;

    /**
     * @Author 89770
     * @Time 2020年12月3日
     * @Description: 根据不同的审批状态，补充审批人条件
     * @Param
     * @return
     */
    public void getReviewerParam(KnowledgeDocumentVersionVo documentVersionVoParam);
    /**
     * @Description: 记录对文档操作（如提交，审核，切换版本，删除版本）
     * @Author: linbq
     * @Date: 2021/2/4 16:06
     * @Params:[knowledgeDocumentId, knowledgeDocumentVersionId, operate, config]
     * @Returns:void
     **/
    public void audit(Long knowledgeDocumentId, Long knowledgeDocumentVersionId, KnowledgeDocumentOperate operate, JSONObject config);
}
