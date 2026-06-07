CREATE TABLE IF NOT EXISTS `knowledge_feishu_sync_config`
(
    `id`                  BIGINT       NOT NULL COMMENT 'ID',
    `name`                VARCHAR(100) NOT NULL COMMENT '配置名称',
    `base_url`            VARCHAR(255) NOT NULL COMMENT '飞书平台地址',
    `app_id`              VARCHAR(100) NOT NULL COMMENT '飞书 App ID',
    `app_secret`          VARCHAR(255) NULL COMMENT '飞书 App Secret',
    `user_access_token`   VARCHAR(1024) NULL COMMENT '飞书 User Access Token',
    `space_id`            VARCHAR(100) NULL COMMENT 'Wiki 空间 ID',
    `space_name`          VARCHAR(255) NULL COMMENT 'Wiki 空间名称',
    `knowledge_circle_id` BIGINT       NOT NULL COMMENT '知识圈 ID',
    `is_active`           TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    `last_sync_status`    VARCHAR(50)  NULL COMMENT '最近同步状态',
    `last_sync_time`      TIMESTAMP(3) NULL COMMENT '最近同步时间',
    `last_sync_audit_id`  BIGINT       NULL COMMENT '最近同步记录 ID',
    `fcu`                 CHAR(32)     NULL COMMENT '创建人',
    `fcd`                 TIMESTAMP(3) NULL COMMENT '创建时间',
    `lcu`                 CHAR(32)     NULL COMMENT '修改人',
    `lcd`                 TIMESTAMP(3) NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_space_id` (`space_id`),
    KEY `idx_circle_id` (`knowledge_circle_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='飞书云文档同步配置';

CREATE TABLE IF NOT EXISTS `knowledge_feishu_document_mapping`
(
    `config_id`                    BIGINT       NOT NULL COMMENT '同步配置 ID',
    `node_token`                   VARCHAR(100) NOT NULL COMMENT 'Wiki 节点 token',
    `obj_token`                    VARCHAR(100) NOT NULL COMMENT '云文档 token',
    `obj_type`                     VARCHAR(50)  NOT NULL COMMENT '云文档类型',
    `knowledge_document_id`        BIGINT       NOT NULL COMMENT '知识文档 ID',
    `knowledge_document_type_uuid` CHAR(32)     NOT NULL COMMENT '知识分类 UUID',
    `title`                        VARCHAR(255) NULL COMMENT '标题',
    `feishu_update_time`           VARCHAR(50)  NULL COMMENT '飞书更新时间',
    `last_sync_time`               TIMESTAMP(3) NULL COMMENT '最近同步时间',
    PRIMARY KEY (`config_id`, `node_token`),
    UNIQUE KEY `uk_document_id` (`knowledge_document_id`),
    KEY `idx_obj_token` (`obj_token`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='飞书云文档与知识文档映射';

CREATE TABLE IF NOT EXISTS `knowledge_feishu_sync_audit`
(
    `id`            BIGINT       NOT NULL COMMENT 'ID',
    `config_id`     BIGINT       NOT NULL COMMENT '同步配置 ID',
    `direction`     VARCHAR(50)  NOT NULL COMMENT '同步方向',
    `status`        VARCHAR(50)  NOT NULL COMMENT '状态',
    `total_count`   INT          NULL COMMENT '总数',
    `success_count` INT          NULL COMMENT '成功数',
    `failed_count`  INT          NULL COMMENT '失败数',
    `error`         TEXT         NULL COMMENT '错误信息',
    `detail`        LONGTEXT     NULL COMMENT '明细 JSON',
    `fcu`           CHAR(32)     NULL COMMENT '创建人',
    `fcd`           TIMESTAMP(3) NULL COMMENT '创建时间',
    `start_time`    TIMESTAMP(3) NULL COMMENT '开始时间',
    `end_time`      TIMESTAMP(3) NULL COMMENT '结束时间',
    PRIMARY KEY (`id`),
    KEY `idx_config_id` (`config_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='飞书云文档同步记录';
