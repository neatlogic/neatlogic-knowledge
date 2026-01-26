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

package neatlogic.module.knowledge.file;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.file.core.FileTypeHandlerBase;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.knowledge.dao.mapper.KnowledgeDocumentMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class KnowledgeFileHandler extends FileTypeHandlerBase {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Override
    public boolean valid(String userUuid, FileVo fileVo, JSONObject jsonObj) {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "知识库附件";
    }

    @Override
    public void afterUpload(FileVo fileVo, JSONObject jsonObj) {
    }

    @Override
    public String getName() {
        return "KNOWLEDGE";
    }

    @Override
    protected boolean myDeleteFile(FileVo fileVo, JSONObject paramObj) {
        return true;
    }

    /**
     * 校验附件是否允许删除
     *
     * @param fileVo 附件信息
     */
    @Override
    public boolean validDeleteFile(FileVo fileVo) {
        Long fileId = knowledgeDocumentMapper.checkKnowledgeDocumentFileIdIsExistsByFileId(fileVo.getId());
        return fileId == null;
    }
}
