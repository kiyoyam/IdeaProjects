package com.campus.errand.common;

/**
 * 业务常量
 */
public final class Constants {

    private Constants() {
    }

    /** 角色 */
    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_RUNNER = "RUNNER";
    public static final String ROLE_ADMIN = "ADMIN";

    /** 任务/订单状态 */
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_FINISHED = "FINISHED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    /** 结算类型 */
    public static final String SETTLE_TASK = "TASK";
    public static final String SETTLE_REFUND = "REFUND";

    /** 提现状态 */
    public static final String WITHDRAW_PENDING = "PENDING";
    public static final String WITHDRAW_APPROVED = "APPROVED";
    public static final String WITHDRAW_REJECTED = "REJECTED";
}
