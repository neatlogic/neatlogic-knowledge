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
