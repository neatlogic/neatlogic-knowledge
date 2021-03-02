package codedriver.module.knowledge.service;

import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.fulltextindex.dto.FullTextIndexVo;
import codedriver.module.knowledge.constvalue.KnowledgeDocumentOperate;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVo;
import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.Map;

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

    /**
     * @Description: 获取截取后的内容
     * @Author: 89770
     * @Date: 2021/3/1 16:47
     * @Params: [knowledgeDocumentVo, contentSb, contentLen]
     * @Returns: void
     **/
    public String getContent( List<KnowledgeDocumentLineVo> lineVoList);

    /**
     * @Description: 一次性获取知识搜索关键字最匹配下标信息,提供给后续循环截取内容和高亮关键字
     * @Author: 89770
     * @Date: 2021/3/2 12:18
     * @Params: [keyword, activeVersionIdList, versionWordOffsetVoMap, versionContentVoMap]
     * @Returns: void
     **/
    public void initVersionWordOffsetAndContentMap(List<String> keywordList, List<Long> activeVersionIdList, Map<Long, FullTextIndexVo> versionWordOffsetVoMap, Map<String, String> versionContentVoMap);
}
