package codedriver.module.knowledge.fulltextindex;

import codedriver.framework.fulltextindex.core.FullTextIndexHandlerBase;
import codedriver.framework.fulltextindex.core.IFullTextIndexType;
import codedriver.framework.fulltextindex.dto.FullTextIndexVo;
import codedriver.module.knowledge.dao.mapper.KnowledgeDocumentMapper;
import codedriver.module.knowledge.dto.KnowledgeDocumentLineVo;
import codedriver.module.knowledge.dto.KnowledgeDocumentVersionVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Title: DocumentLineFullTextIndexHandler
 * @Package: codedriver.module.knowledge.fulltextindex
 * @Description: 知识库索引处理器
 * @author: chenqiwei
 * @date: 2021/2/264:36 下午
 * Copyright(c) 2021 TechSure Co.,Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 **/
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
            if(StringUtils.isNotBlank(line.getContent())) {
                sb.append(line.getContent());
            }
        }
        fullTextIndexVo.addFieldContent("content", sb.toString());
        KnowledgeDocumentVersionVo versionVo = knowledgeDocumentMapper.getKnowledgeDocumentVersionById(fullTextIndexVo.getTargetId());
        fullTextIndexVo.addFieldContent("title",versionVo.getTitle());
    }

    @Override
    public IFullTextIndexType getType() {
        return FullTextIndexType.KNOW_DOCUMENT_VERSION;
    }

    @Override
    public void rebuildIndex(Boolean isRebuildAll) {

    }
}
