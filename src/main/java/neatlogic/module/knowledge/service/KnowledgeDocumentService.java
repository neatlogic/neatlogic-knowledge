/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.knowledge.service;

import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.fulltextindex.dto.fulltextindex.FullTextIndexVo;
import neatlogic.framework.knowledge.constvalue.KnowledgeDocumentOperate;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentLineVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVersionVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVo;
import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.Map;

public interface KnowledgeDocumentService {

    public int isDeletable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);
    
    public int isEditable(KnowledgeDocumentVersionVo knowledgeDocumentVersionVo);
    
    public int isReviewer(Long knowledgeCircleId);

    public int isMember(Long knowledgeCircleId);
    
    public KnowledgeDocumentVo getKnowledgeDocumentDetailByKnowledgeDocumentVersionId(Long knowledgeDocumentVersionId) throws PermissionDeniedException;

    public KnowledgeDocumentVo getKnowledgeDocumentContentByKnowledgeDocumentVersionId(Long knowledgeDocumentVersionId) throws PermissionDeniedException;

    public Long checkViewPermissionByDocumentIdAndVersionId(Long documentId,Long versionId) throws Exception;

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
    public void initVersionWordOffsetAndContentMap(List<String> keywordList, List<Long> activeVersionIdList, Map<Long, FullTextIndexVo> versionWordOffsetVoMap, Map<Long, String> versionContentVoMap);

    /**
     * @Description: 统计非草稿的版本状态数量
     * @Author: 89770
     * @Date: 2021/3/2 12:04
     * @Params: [documentVersionVoParam, resultJson]
     * @Returns: void
     **/
    public void setTitleAndShortcutContentHighlight(List<String> keywordList, Long versionId,Object documentObj,Map<Long, FullTextIndexVo> versionIndexVoMap,Map<Long, String> versionContentVoMap);

    /**
     * @Description: 一次性获取知识搜索关键字最匹配下标信息,提供给后续循环截取内容和高亮关键字
     * @Author: 89770
     * @Date: 2021/3/2 17:47
     * @Params: [keywordList, activeVersionIdList]
     * @Returns: void
     **/
    public void setVersionContentMap(List<String> keywordList,List<Long> activeVersionIdList,Map<Long, FullTextIndexVo> versionIndexVoMap,Map<Long, String> versionContentMap);
}
