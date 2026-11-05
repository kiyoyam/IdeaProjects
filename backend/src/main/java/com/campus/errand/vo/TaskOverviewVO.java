package com.campus.errand.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 工作台统计视图对象
 * <p>
 * 一个接口同时服务三处：顶部菜单角标（activeCount）、工作台数据卡片、任务广场概览条。
 */
@Data
public class TaskOverviewVO {

    /** 当前用户进行中的接单数（已接单 + 配送中），用于菜单角标 */
    private Long activeCount = 0L;
    /** 当前用户已完成接单数 */
    private Long completedCount = 0L;
    /** 当前用户已取消接单数 */
    private Long cancelledCount = 0L;
    /** 当前用户今日接单数 */
    private Long todayCount = 0L;
    /** 当前用户累计跑腿收入 */
    private BigDecimal income = BigDecimal.ZERO;
    /** 当前用户平均评分 */
    private BigDecimal rating;
    /** 被评价次数 */
    private Integer ratingCount = 0;
    /** 当前用户发布且未结束的任务数 */
    private Long publishedActive = 0L;
    /** 当前用户累计发布任务数 */
    private Long publishedTotal = 0L;
    /** 广场待接单任务总数 */
    private Long pendingCount = 0L;
    /** 广场今日完成任务总数 */
    private Long todayCompleted = 0L;
}
