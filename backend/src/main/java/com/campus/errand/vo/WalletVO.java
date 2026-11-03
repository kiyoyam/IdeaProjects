package com.campus.errand.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 钱包视图对象
 */
@Data
public class WalletVO {

    private BigDecimal balance;
    private BigDecimal frozenBalance;
    /** 累计收入（跑腿结算到账） */
    private BigDecimal totalIncome;
    /** 累计支出（发布任务支出） */
    private BigDecimal totalExpense;
    /** 累计提现 */
    private BigDecimal totalWithdrawn;
    /** 完成接单数 */
    private Long completedOrders;
}
