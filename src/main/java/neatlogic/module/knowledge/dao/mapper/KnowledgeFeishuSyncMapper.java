/*
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package neatlogic.module.knowledge.dao.mapper;

import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeiShuDocumentMappingVo;
import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncAuditVo;
import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncConfigVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KnowledgeFeishuSyncMapper {

    List<KnowledgeFeiShuDocumentMappingVo> getSyncDocumentListByNodeTokenList(List<String> nodeTokenList);

    KnowledgeFeiShuDocumentMappingVo getSyncDocumentByNodeToken(String nodeToken);

    int insertSyncDocument(KnowledgeFeiShuDocumentMappingVo vo);

    int updateSyncDocument(KnowledgeFeiShuDocumentMappingVo vo);

    int updateSyncDocumentStatusByNodeToken(@Param("nodeToken") String nodeToken, @Param("status") String value);

    int insertSyncMediasMapping(@Param("uuid") String uuid, @Param("fileId") Long fileId);

    Long getSyncMediasMappingFileIdByUuid(String uuid);
}
