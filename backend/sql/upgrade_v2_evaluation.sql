-- ============================================================
-- 评价模块升级脚本（v2）
-- 在已有 campus_errand 库上执行一次即可；新建库直接跑 campus_errand.sql 就不需要它
-- 用途：为评价表补充多维度评分、标签、匿名标记
-- ============================================================
USE `campus_errand`;

ALTER TABLE `evaluation`
    ADD COLUMN `speed_rating`   TINYINT      DEFAULT NULL COMMENT '配送速度评分1-5' AFTER `rating`,
    ADD COLUMN `service_rating` TINYINT      DEFAULT NULL COMMENT '服务态度评分1-5' AFTER `speed_rating`,
    ADD COLUMN `quality_rating` TINYINT      DEFAULT NULL COMMENT '完成质量评分1-5' AFTER `service_rating`,
    ADD COLUMN `tags`           VARCHAR(255) DEFAULT NULL COMMENT '快捷标签，逗号分隔' AFTER `content`,
    ADD COLUMN `anonymous`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否匿名 1是0否' AFTER `tags`;
