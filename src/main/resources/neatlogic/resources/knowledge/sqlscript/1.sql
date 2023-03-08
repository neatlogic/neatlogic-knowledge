-- ----------------------------
-- Table structure for knowledge_circle
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_circle` (
  `id` bigint NOT NULL COMMENT '主键',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识圈表';

-- ----------------------------
-- Table structure for knowledge_circle_user
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_circle_user` (
  `knowledge_circle_id` bigint NOT NULL COMMENT '知识圈ID',
  `uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户/角色/分组UUID',
  `type` enum('common','user','team','role') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型',
  `auth_type` enum('approver','member') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '区分审批人与成员字段（approver：审批人；member：成员）',
  PRIMARY KEY (`knowledge_circle_id`,`uuid`,`type`,`auth_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识圈用户表';

-- ----------------------------
-- Table structure for knowledge_document
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_document` (
  `id` bigint NOT NULL COMMENT '文档主键id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标题',
  `knowledge_document_version_id` bigint DEFAULT NULL COMMENT '激活版本id',
  `knowledge_document_type_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分类id',
  `knowledge_circle_id` bigint DEFAULT NULL COMMENT '知识圈id',
  `is_delete` tinyint(1) DEFAULT '0' COMMENT '是否删除',
  `version` int DEFAULT NULL COMMENT '激活版本号',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文档来源',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `source_idx` (`source`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识文档';

-- ----------------------------
-- Table structure for knowledge_document_audit
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_document_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `knowledge_document_id` bigint DEFAULT NULL COMMENT '知识id',
  `knowledge_document_version_id` bigint DEFAULT NULL COMMENT '知识版本id',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `operate` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `config_hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '配置hash',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_document` (`knowledge_document_id`,`knowledge_document_version_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=151 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库操作记录表';

-- ----------------------------
-- Table structure for knowledge_document_audit_detail
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_document_audit_detail` (
  `hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '记录hash',
  `config` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '记录详细',
  PRIMARY KEY (`hash`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库操作记录详情表';

-- ----------------------------
-- Table structure for knowledge_document_collect
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_document_collect` (
  `knowledge_document_id` bigint NOT NULL COMMENT '文档主键',
  `user_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户UUID',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '收藏时间',
  PRIMARY KEY (`knowledge_document_id`,`user_uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识文档收集';

-- ----------------------------
-- Table structure for knowledge_document_favor
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_document_favor` (
  `knowledge_document_id` bigint NOT NULL COMMENT '文档主键',
  `user_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户UUID',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '点赞时间',
  PRIMARY KEY (`knowledge_document_id`,`user_uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识文档收藏';

-- ----------------------------
-- Table structure for knowledge_document_file
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_document_file` (
  `knowledge_document_id` bigint NOT NULL COMMENT '文档id',
  `knowledge_document_version_id` bigint NOT NULL COMMENT '文档版本id',
  `file_id` bigint NOT NULL COMMENT '附件id',
  PRIMARY KEY (`knowledge_document_id`,`knowledge_document_version_id`,`file_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识文档附件';

-- ----------------------------
-- Table structure for knowledge_document_invoke
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_document_invoke` (
  `knowledge_document_id` bigint NOT NULL COMMENT '知识文档id',
  `invoke_id` bigint NOT NULL COMMENT '调用者id',
  `source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '来源',
  PRIMARY KEY (`knowledge_document_id`) USING BTREE,
  UNIQUE KEY `idx_invoke_id` (`invoke_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识文档调用';

-- ----------------------------
-- Table structure for knowledge_document_line
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_document_line` (
  `uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '行uuid',
  `handler` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '行组件',
  `knowledge_document_id` bigint DEFAULT NULL COMMENT '文档id',
  `knowledge_document_version_id` bigint DEFAULT NULL COMMENT '文档版本id',
  `content_hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '内容hash',
  `config_hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '配置hash',
  `line_number` int DEFAULT NULL COMMENT '行号',
  PRIMARY KEY (`uuid`) USING BTREE,
  KEY `idx_document_id` (`knowledge_document_id`) USING BTREE,
  KEY `idx_document_version_id` (`knowledge_document_version_id`) USING BTREE,
  KEY `idx_document_linenum` (`line_number`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识文档行';

-- ----------------------------
-- Table structure for knowledge_document_line_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_document_line_config` (
  `hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `config` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置',
  PRIMARY KEY (`hash`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识文档行配置';

-- ----------------------------
-- Table structure for knowledge_document_line_content
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_document_line_content` (
  `hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'hash值',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '文本内容',
  PRIMARY KEY (`hash`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识文档行内容';

-- ----------------------------
-- Table structure for knowledge_document_tag
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_document_tag` (
  `knowledge_document_id` bigint NOT NULL COMMENT '文档id',
  `knowledge_document_version_id` bigint NOT NULL COMMENT '文档版本id',
  `tag_id` bigint NOT NULL COMMENT '标签id',
  PRIMARY KEY (`knowledge_document_id`,`knowledge_document_version_id`,`tag_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识文档标签';

-- ----------------------------
-- Table structure for knowledge_document_type
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_document_type` (
  `uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `knowledge_circle_id` bigint NOT NULL COMMENT '知识圈ID',
  `parent_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '父类型ID',
  `lft` int DEFAULT NULL COMMENT '左编码',
  `rht` int DEFAULT NULL COMMENT '右编码',
  `sort` int DEFAULT NULL COMMENT '排序（相对于同级节点的顺序）',
  PRIMARY KEY (`uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识类型表';

-- ----------------------------
-- Table structure for knowledge_document_version
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_document_version` (
  `id` bigint NOT NULL COMMENT '文档版本主键id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标题',
  `knowledge_document_type_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文档类型uuid',
  `knowledge_document_id` bigint DEFAULT NULL COMMENT '文档id',
  `from_version` int DEFAULT NULL COMMENT '原版本号',
  `version` int DEFAULT NULL COMMENT '版本号',
  `status` enum('draft','submitted','rejected','passed','expired') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态，(草稿、提交审核、审核通过)',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后修改人',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '最后修改时间',
  `size` int DEFAULT NULL COMMENT '文档大小，单位字节byte',
  `reviewer` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批人',
  `review_time` timestamp(3) NULL DEFAULT NULL COMMENT '审批时间',
  `is_delete` tinyint(1) DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_knowledge_document_id` (`knowledge_document_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库版本表';

-- ----------------------------
-- Table structure for knowledge_document_view_count
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_document_view_count` (
  `knowledge_document_id` bigint NOT NULL COMMENT '文档主键',
  `count` int DEFAULT NULL COMMENT '浏览量',
  PRIMARY KEY (`knowledge_document_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识查看次数';

-- ----------------------------
-- Table structure for knowledge_tag
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_tag` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标签名',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_name_unique` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库标签表';

-- ----------------------------
-- Table structure for knowledge_template
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_template` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模版名称',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目录',
  `is_active` tinyint NOT NULL COMMENT '是否激活',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人ID',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人ID',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_lcd` (`lcd`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库模版';