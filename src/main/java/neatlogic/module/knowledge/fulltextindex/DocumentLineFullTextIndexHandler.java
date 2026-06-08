/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.knowledge.fulltextindex;

import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.asynchronization.threadpool.CachedThreadPool;
import neatlogic.framework.fulltextindex.core.FullTextIndexHandlerBase;
import neatlogic.framework.fulltextindex.core.IFullTextIndexType;
import neatlogic.framework.fulltextindex.dto.fulltextindex.FullTextIndexTypeVo;
import neatlogic.framework.fulltextindex.dto.fulltextindex.FullTextIndexVo;
import neatlogic.framework.fulltextindex.dto.globalsearch.DocumentVo;
import neatlogic.framework.knowledge.constvalue.KnowledgeFullTextIndexType;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentLineVo;
import neatlogic.framework.knowledge.dto.KnowledgeDocumentVersionVo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.Semaphore;

@Service
public class DocumentLineFullTextIndexHandler extends FullTextIndexHandlerBase {
    private static final Semaphore semaphore = new Semaphore(5);
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
        if (versionVo != null && StringUtils.isNotBlank(versionVo.getTitle())) {
            fullTextIndexVo.addFieldContent("title", new FullTextIndexVo.WordVo(versionVo.getTitle()));
        }
    }

    @Override
    public boolean needSaveContent() {
        return true;
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
        fullTextIndexTypeVo.setPageSize(500);
        fullTextIndexTypeVo.setCurrentPage(1);
        //为了增量重建索引时，能实现补充缺少属性的效果，因此不管全量重建还是增量重建，都需要遍历所有知识库
        List<Long> knowledgeVersionIdList = knowledgeDocumentMapper.getKnowledgeDocumentVersionIdListForFulltextIndex(fullTextIndexTypeVo);
        while (CollectionUtils.isNotEmpty(knowledgeVersionIdList)) {
            for (Long knowledgeVersionId : knowledgeVersionIdList) {
                try {
                    semaphore.acquire();
                    CachedThreadPool.execute(new NeatLogicThread("FULLTEXTINDEX-REBUILD-KNOWLEDGE-VERSION-" + knowledgeVersionId) {
                        @Override
                        protected void execute() {
                            try {
                                createIndex(knowledgeVersionId, true);
                            } finally {
                                semaphore.release();
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            fullTextIndexTypeVo.setCurrentPage(fullTextIndexTypeVo.getCurrentPage() + 1);
            knowledgeVersionIdList = knowledgeDocumentMapper.getKnowledgeDocumentVersionIdListForFulltextIndex(fullTextIndexTypeVo);
        }
    }

}
