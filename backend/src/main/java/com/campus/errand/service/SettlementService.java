package com.campus.errand.service;

import com.campus.errand.common.PageResult;
import com.campus.errand.dto.WithdrawDTO;
import com.campus.errand.entity.Task;
import com.campus.errand.vo.SettlementVO;
import com.campus.errand.vo.WalletVO;
import com.campus.errand.vo.WithdrawalVO;

/**
 * 结算服务
 */
public interface SettlementService {

    /** 任务完成结算：冻结余额转入跑腿员余额，生成结算记录 */
    void settle(Task task);

    /** 任务取消退款：冻结余额退回发布者，生成退款记录 */
    void refund(Task task, String reason);

    /** 申请提现 */
    void withdraw(Long userId, WithdrawDTO dto);

    WalletVO wallet(Long userId);

    PageResult<SettlementVO> mySettlements(Long userId, Integer page, Integer size);

    PageResult<WithdrawalVO> myWithdrawals(Long userId, Integer page, Integer size);

    PageResult<SettlementVO> adminSettlements(Integer page, Integer size, String type);

    PageResult<WithdrawalVO> adminWithdrawals(Integer page, Integer size, String status);

    void approveWithdrawal(Long id);

    void rejectWithdrawal(Long id, String remark);
}
