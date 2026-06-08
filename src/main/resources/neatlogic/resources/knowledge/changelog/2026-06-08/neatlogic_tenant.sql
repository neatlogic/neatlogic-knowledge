CREATE TABLE IF NOT EXISTS `knowledge_feishu_document_mapping` (
    `app_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '飞书应用ID',
    `space_id` bigint NOT NULL COMMENT '知识库空间ID',
    `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题',
    `parent_node_token` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父节点token',
    `node_token` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Wiki 节点 token',
    `obj_token` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '云文档 token',
    `obj_type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '云文档类型',
    `update_time` timestamp(3) NOT NULL COMMENT '飞书更新时间',
    `knowledge_document_type_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '知识分类 UUID',
    `knowledge_document_id` bigint DEFAULT NULL COMMENT '知识文档 ID',
    `knowledge_document_version_id` bigint DEFAULT NULL COMMENT '知识文档版本ID',
    `status` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态',
    `config` longtext COLLATE utf8mb4_general_ci COMMENT '信息',
    `lcu` char(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
    `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (`node_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='飞书云文档与知识文档映射';

CREATE TABLE IF NOT EXISTS `knowledge_feishu_medias_mapping` (
    `file_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '唯一标识',
    `file_id` bigint NOT NULL COMMENT '附件id',
    PRIMARY KEY (`file_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
