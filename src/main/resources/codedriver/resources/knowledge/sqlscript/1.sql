/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
 SET FOREIGN_KEY_CHECKS=0;
 
 CREATE TABLE `knowledge_circle`  (
   `id` bigint NOT NULL COMMENT '主键',
   `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
   PRIMARY KEY (`id`) USING BTREE
 ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识圈表' ROW_FORMAT = Dynamic;
 
 CREATE TABLE `knowledge_circle_user`  (
   `knowledge_circle_id` bigint NOT NULL COMMENT '知识圈ID',
   `uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户/角色/分组UUID',
   `type` enum('common','user','team','role') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型',
   `auth_type` enum('approver','member') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '区分审批人与成员字段（approver：审批人；member：成员）',
   PRIMARY KEY (`knowledge_circle_id`, `uuid`, `type`, `auth_type`) USING BTREE
 ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识圈用户表' ROW_FORMAT = Dynamic;
 
 CREATE TABLE `knowledge_document`  (
   `id` bigint NOT NULL COMMENT '文档主键id',
   `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标题',
   `knowledge_document_version_id` bigint NULL DEFAULT NULL COMMENT '激活版本id',
   `knowledge_document_type_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分类id',
   `knowledge_circle_id` bigint NULL DEFAULT NULL COMMENT '知识圈id',
   `is_delete` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
   `version` int NULL DEFAULT NULL COMMENT '激活版本号',
   `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
   `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
   `source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文档来源',
   PRIMARY KEY (`id`) USING BTREE,
   INDEX `source_idx`(`source`) USING BTREE
 ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识文档' ROW_FORMAT = Dynamic;
 
 CREATE TABLE `knowledge_document_audit`  (
   `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
   `knowledge_document_id` bigint NULL DEFAULT NULL COMMENT '知识id',
   `knowledge_document_version_id` bigint NULL DEFAULT NULL COMMENT '知识版本id',
   `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
   `operate` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作',
   `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
   `config_hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '配置hash',
   PRIMARY KEY (`id`) USING BTREE,
   INDEX `idx_document`(`knowledge_document_id`, `knowledge_document_version_id`) USING BTREE
 ) ENGINE = InnoDB AUTO_INCREMENT = 150 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识库操作记录表' ROW_FORMAT = Dynamic;
 
 CREATE TABLE `knowledge_document_audit_detail`  (
   `hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '记录hash',
   `config` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '记录详细',
   PRIMARY KEY (`hash`) USING BTREE
 ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识库操作记录详情表' ROW_FORMAT = Dynamic;
 
 CREATE TABLE `knowledge_document_collect`  (
   `knowledge_document_id` bigint NOT NULL COMMENT '文档主键',
   `user_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户UUID',
   `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '收藏时间',
   PRIMARY KEY (`knowledge_document_id`, `user_uuid`) USING BTREE
 ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识文档收集' ROW_FORMAT = Dynamic;
 
 CREATE TABLE `knowledge_document_favor`  (
   `knowledge_document_id` bigint NOT NULL COMMENT '文档主键',
   `user_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户UUID',
   `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '点赞时间',
   PRIMARY KEY (`knowledge_document_id`, `user_uuid`) USING BTREE
 ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识文档收藏' ROW_FORMAT = Dynamic;
 
 CREATE TABLE `knowledge_document_file`  (
   `knowledge_document_id` bigint NOT NULL COMMENT '文档id',
   `knowledge_document_version_id` bigint NOT NULL COMMENT '文档版本id',
   `file_id` bigint NOT NULL COMMENT '附件id',
   PRIMARY KEY (`knowledge_document_id`, `knowledge_document_version_id`, `file_id`) USING BTREE
 ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识文档附件' ROW_FORMAT = Dynamic;
 
 CREATE TABLE `knowledge_document_invoke`  (
   `knowledge_document_id` bigint NOT NULL COMMENT '知识文档id',
   `invoke_id` bigint NOT NULL COMMENT '调用者id',
   `source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '来源',
   PRIMARY KEY (`knowledge_document_id`) USING BTREE,
   UNIQUE INDEX `idx_invoke_id`(`invoke_id`) USING BTREE
 ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识文档调用' ROW_FORMAT = Dynamic;
 
 CREATE TABLE `knowledge_document_line`  (
   `uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '行uuid',
   `handler` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '行组件',
   `knowledge_document_id` bigint NULL DEFAULT NULL COMMENT '文档id',
   `knowledge_document_version_id` bigint NULL DEFAULT NULL COMMENT '文档版本id',
   `content_hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '内容hash',
   `config_hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '配置hash',
   `line_number` int NULL DEFAULT NULL COMMENT '行号',
   PRIMARY KEY (`uuid`) USING BTREE,
   INDEX `idx_document_id`(`knowledge_document_id`) USING BTREE,
   INDEX `idx_document_version_id`(`knowledge_document_version_id`) USING BTREE,
   INDEX `idx_document_linenum`(`line_number`) USING BTREE
 ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识文档行' ROW_FORMAT = Dynamic;
 
 CREATE TABLE `knowledge_document_line_config`  (
   `hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
   `config` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '配置',
   PRIMARY KEY (`hash`) USING BTREE
 ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识文档行配置' ROW_FORMAT = Dynamic;
 
 CREATE TABLE `knowledge_document_line_content`  (
   `hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'hash值',
   `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '文本内容',
   PRIMARY KEY (`hash`) USING BTREE
 ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识文档行内容' ROW_FORMAT = Dynamic;
 
 CREATE TABLE `knowledge_document_tag`  (
   `knowledge_document_id` bigint NOT NULL COMMENT '文档id',
   `knowledge_document_version_id` bigint NOT NULL COMMENT '文档版本id',
   `tag_id` bigint NOT NULL COMMENT '标签id',
   PRIMARY KEY (`knowledge_document_id`, `knowledge_document_version_id`, `tag_id`) USING BTREE
 ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识文档标签' ROW_FORMAT = Dynamic;
 
 CREATE TABLE `knowledge_document_type`  (
   `uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
   `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
   `knowledge_circle_id` bigint NOT NULL COMMENT '知识圈ID',
   `parent_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '父类型ID',
   `lft` int NULL DEFAULT NULL COMMENT '左编码',
   `rht` int NULL DEFAULT NULL COMMENT '右编码',
   `sort` int NULL DEFAULT NULL COMMENT '排序（相对于同级节点的顺序）',
   PRIMARY KEY (`uuid`) USING BTREE
 ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识类型表' ROW_FORMAT = Dynamic;
 
 CREATE TABLE `knowledge_document_version`  (
   `id` bigint NOT NULL COMMENT '文档版本主键id',
   `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标题',
   `knowledge_document_type_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文档类型uuid',
   `knowledge_document_id` bigint NULL DEFAULT NULL COMMENT '文档id',
   `from_version` int NULL DEFAULT NULL COMMENT '原版本号',
   `version` int NULL DEFAULT NULL COMMENT '版本号',
   `status` enum('draft','submitted','rejected','passed','expired') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '状态，(草稿、提交审核、审核通过)',
   `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后修改人',
   `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '最后修改时间',
   `size` int NULL DEFAULT NULL COMMENT '文档大小，单位字节byte',
   `reviewer` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '审批人',
   `review_time` timestamp(3) NULL DEFAULT NULL COMMENT '审批时间',
   `is_delete` tinyint(1) NULL DEFAULT 0 COMMENT '是否已删除',
   PRIMARY KEY (`id`) USING BTREE,
   INDEX `idx_knowledge_document_id`(`knowledge_document_id`) USING BTREE
 ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识库版本表' ROW_FORMAT = Dynamic;
 
 CREATE TABLE `knowledge_document_view_count`  (
   `knowledge_document_id` bigint NOT NULL COMMENT '文档主键',
   `count` int NULL DEFAULT NULL COMMENT '浏览量',
   PRIMARY KEY (`knowledge_document_id`) USING BTREE
 ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识查看次数' ROW_FORMAT = Dynamic;
 
 CREATE TABLE `knowledge_tag`  (
   `id` bigint NOT NULL COMMENT 'id',
   `name` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标签名',
   PRIMARY KEY (`id`) USING BTREE,
   UNIQUE INDEX `idx_name_unique`(`name`) USING BTREE
 ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识库标签表' ROW_FORMAT = Dynamic;
 
 CREATE TABLE `knowledge_template`  (
   `id` bigint NOT NULL COMMENT '主键ID',
   `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模版名称',
   `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目录',
   `is_active` tinyint NOT NULL COMMENT '是否激活',
   `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人ID',
   `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人ID',
   `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
   `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '更新时间',
   PRIMARY KEY (`id`) USING BTREE,
   INDEX `idx_lcd`(`lcd`) USING BTREE
 ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识库模版' ROW_FORMAT = Dynamic;
 
 SET FOREIGN_KEY_CHECKS=1;