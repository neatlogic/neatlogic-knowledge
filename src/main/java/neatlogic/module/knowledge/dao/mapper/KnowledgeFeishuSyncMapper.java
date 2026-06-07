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

import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncAuditVo;
import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncConfigVo;
import neatlogic.framework.knowledge.dto.feishu.KnowledgeFeishuSyncDocumentVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KnowledgeFeishuSyncMapper {

    int searchConfigCount(KnowledgeFeishuSyncConfigVo vo);

    List<KnowledgeFeishuSyncConfigVo> searchConfig(KnowledgeFeishuSyncConfigVo vo);

    KnowledgeFeishuSyncConfigVo getConfigById(Long id);

    KnowledgeFeishuSyncConfigVo getConfigByName(String name);

    int checkNameIsRepeat(KnowledgeFeishuSyncConfigVo vo);

    int insertConfig(KnowledgeFeishuSyncConfigVo vo);

    int updateConfig(KnowledgeFeishuSyncConfigVo vo);

    int updateConfigStatus(@Param("id") Long id, @Param("isActive") Integer isActive, @Param("lcu") String lcu);

//    int updateConfigLastSync(KnowledgeFeishuSyncConfigVo vo);

    int deleteConfig(Long id);

    int insertAudit(KnowledgeFeishuSyncAuditVo vo);

    int updateAudit(KnowledgeFeishuSyncAuditVo vo);

    KnowledgeFeishuSyncAuditVo getAuditById(Long id);

    int searchAuditCount(KnowledgeFeishuSyncAuditVo vo);

    List<KnowledgeFeishuSyncAuditVo> searchAudit(KnowledgeFeishuSyncAuditVo vo);

    List<KnowledgeFeishuSyncDocumentVo> getSyncDocumentListByNodeTokenList(List<String> nodeTokenList);

    KnowledgeFeishuSyncDocumentVo getSyncDocumentByNodeToken(String nodeToken);

//    KnowledgeFeishuSyncDocumentVo getSyncDocumentByDocumentId(Long knowledgeDocumentId);

    int insertSyncDocument(KnowledgeFeishuSyncDocumentVo vo);

    int updateSyncDocument(KnowledgeFeishuSyncDocumentVo vo);

    int insertSyncMediasMapping(@Param("uuid") String uuid, @Param("fileId") Long fileId);

    Long getSyncMediasMappingFileIdByUuid(String uuid);

    int updateSyncDocumentStatusByNodeToken(@Param("nodeToken") String nodeToken, @Param("status") String value);
}
