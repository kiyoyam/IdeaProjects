-- =============================================================
-- 校园跑腿任务发布与接单系统 数据库初始化脚本
-- 数据库：campus_errand  (MySQL 8.0, utf8mb4)
-- 说明：初始用户/示例任务由后端 DataInitializer 自动写入，
--       无需在此脚本中插入用户数据。
-- =============================================================

CREATE DATABASE IF NOT EXISTS campus_errand DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE campus_errand;

-- 用户表（学生/跑腿员/管理员）
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`       VARCHAR(50)   NOT NULL COMMENT '登录账号',
    `password`       VARCHAR(100)  NOT NULL COMMENT 'BCrypt加密密码',
    `nickname`       VARCHAR(50)   DEFAULT NULL COMMENT '昵称',
    `phone`          VARCHAR(20)   DEFAULT NULL COMMENT '手机号',
    `avatar`         VARCHAR(255)  DEFAULT NULL COMMENT '头像URL',
    `role`           VARCHAR(20)   NOT NULL DEFAULT 'STUDENT' COMMENT '角色:STUDENT/RUNNER/ADMIN',
    `balance`        DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '可用余额',
    `frozen_balance` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '冻结余额(发布任务未结算金额)',
    `rating`         DECIMAL(3,2)  NOT NULL DEFAULT 5.00 COMMENT '平均评分',
    `rating_count`   INT           NOT NULL DEFAULT 0 COMMENT '被评次数',
    `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态:1正常/0禁用',
    `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- 任务表
DROP TABLE IF EXISTS `task`;
CREATE TABLE `task` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `publisher_id`    BIGINT        NOT NULL COMMENT '发布者ID',
    `runner_id`       BIGINT        DEFAULT NULL COMMENT '接单跑腿员ID',
    `title`           VARCHAR(100)  NOT NULL COMMENT '任务标题',
    `description`     VARCHAR(1000) DEFAULT NULL COMMENT '任务描述',
    `category`        VARCHAR(20)   NOT NULL DEFAULT 'OTHER' COMMENT '分类:PICKUP/DELIVERY/BUY/HELP/OTHER',
    `reward`          DECIMAL(12,2) NOT NULL COMMENT '报酬金额',
    `pickup_address`  VARCHAR(200)  NOT NULL COMMENT '取件地址',
    `delivery_address` VARCHAR(200) NOT NULL COMMENT '送达地址',
    `deadline`        DATETIME      DEFAULT NULL COMMENT '截止时间',
    `status`          VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态:PENDING/ACCEPTED/IN_PROGRESS/FINISHED/COMPLETED/CANCELLED',
    `cancel_reason`   VARCHAR(255)  DEFAULT NULL COMMENT '取消原因',
    `create_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `complete_time`   DATETIME      DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    KEY `idx_publisher` (`publisher_id`),
    KEY `idx_runner` (`runner_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='任务表';

-- 接单记录表（跑腿员接单后生成）
DROP TABLE IF EXISTS `errand_order`;
CREATE TABLE `errand_order` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `task_id`      BIGINT        NOT NULL COMMENT '任务ID',
    `publisher_id` BIGINT        NOT NULL COMMENT '发布者ID',
    `runner_id`    BIGINT        NOT NULL COMMENT '跑腿员ID',
    `reward`       DECIMAL(12,2) NOT NULL COMMENT '订单金额',
    `status`       VARCHAR(20)   NOT NULL COMMENT '状态:ACCEPTED/IN_PROGRESS/FINISHED/COMPLETED/CANCELLED',
    `create_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `complete_time` DATETIME     DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task` (`task_id`),
    KEY `idx_runner` (`runner_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='接单记录表';

-- 结算记录表
DROP TABLE IF EXISTS `settlement`;
CREATE TABLE `settlement` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '结算ID',
    `task_id`      BIGINT        NOT NULL COMMENT '任务ID',
    `order_id`     BIGINT        DEFAULT NULL COMMENT '接单记录ID',
    `publisher_id` BIGINT        NOT NULL COMMENT '发布者ID',
    `runner_id`    BIGINT        NOT NULL COMMENT '跑腿员ID',
    `amount`       DECIMAL(12,2) NOT NULL COMMENT '结算金额',
    `type`         VARCHAR(20)   NOT NULL DEFAULT 'TASK' COMMENT '类型:TASK任务结算/REFUND退款',
    `status`       VARCHAR(20)   NOT NULL DEFAULT 'SUCCESS' COMMENT '状态:SUCCESS',
    `remark`       VARCHAR(255)  DEFAULT NULL COMMENT '备注',
    `create_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_publisher` (`publisher_id`),
    KEY `idx_runner` (`runner_id`),
    KEY `idx_task` (`task_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='结算记录表';

-- 提现申请表
DROP TABLE IF EXISTS `withdrawal`;
CREATE TABLE `withdrawal` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '提现ID',
    `user_id`     BIGINT        NOT NULL COMMENT '申请用户ID',
    `amount`      DECIMAL(12,2) NOT NULL COMMENT '提现金额',
    `status`      VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态:PENDING/APPROVED/REJECTED',
    `remark`      VARCHAR(255)  DEFAULT NULL COMMENT '备注(驳回原因等)',
    `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `handle_time` DATETIME      DEFAULT NULL COMMENT '处理时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='提现申请表';

-- 评价表（任务完成后双方互评，每人每任务一次）
DROP TABLE IF EXISTS `evaluation`;
CREATE TABLE `evaluation` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `task_id`      BIGINT       NOT NULL COMMENT '任务ID',
    `evaluator_id` BIGINT       NOT NULL COMMENT '评价人ID',
    `evaluatee_id` BIGINT       NOT NULL COMMENT '被评价人ID',
    `rating`       INT          NOT NULL COMMENT '评分1-5',
    `content`      VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_evaluator` (`task_id`, `evaluator_id`),
    KEY `idx_evaluatee` (`evaluatee_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='评价表';
