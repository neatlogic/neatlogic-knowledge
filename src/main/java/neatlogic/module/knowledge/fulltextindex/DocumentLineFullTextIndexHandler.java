/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.knowledge.fulltextindex;

import neatlogic.framework.fulltextindex.core.FullTextIndexHandlerBase;
import neatlogic.framework.fulltextindex.core.IFullTextIndexType;
import neatlogic.framework.fulltextindex.dto.fulltextindex.FullTextIndexTypeVo;
import neatlogic.framework.fulltextindex.dto.fulltextindex.FullTextIndexVo;
import neatlogic.framework.fulltextindex.dto.globalsearch.DocumentVo;
import neatlogic.framework.knowledge.constvalue.KnowledgeFullTextIndexType;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentLineVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVersionVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class DocumentLineFullTextIndexHandler extends FullTextIndexHandlerBase {
    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Override
    protected String getModuleId() {
        return "knowledge";
    }

    @Override
    protected void myCreateIndex(FullTextIndexVo fullTextIndexVo) {
        List<KnowledgeDocumentLineVo> lineList = knowledgeDocumentMapper.getKnowledgeDocumentLineListByKnowledgeDocumentVersionId(fullTextIndexVo.getTargetId());
        StringBuilder sb = new StringBuilder();
        for (KnowledgeDocumentLineVo line : lineList) {
            if (StringUtils.isNotBlank(line.getContent())) {
                sb.append(line.getContent());
            }
        }
        fullTextIndexVo.addFieldContent("content", new FullTextIndexVo.WordVo(sb.toString()));
        KnowledgeDocumentVersionVo versionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(fullTextIndexVo.getTargetId());
        fullTextIndexVo.addFieldContent("title", new FullTextIndexVo.WordVo(versionVo.getTitle()));
    }

    @Override
    protected void myMakeupDocument(DocumentVo documentVo) {

    }

    @Override
    public IFullTextIndexType getType() {
        return KnowledgeFullTextIndexType.KNOW_DOCUMENT_VERSION;
    }

    @Override
    public void myRebuildIndex(FullTextIndexTypeVo fullTextIndexTypeVo) {

    }

}