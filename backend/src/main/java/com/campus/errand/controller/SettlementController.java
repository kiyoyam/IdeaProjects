package com.campus.errand.controller;

import com.campus.errand.common.PageResult;
import com.campus.errand.common.Result;
import com.campus.errand.dto.WithdrawDTO;
import com.campus.errand.security.UserContext;
import com.campus.errand.service.SettlementService;
import com.campus.errand.vo.SettlementVO;
import com.campus.errand.vo.WalletVO;
import com.campus.errand.vo.WithdrawalVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 结算与钱包接口：结算记录 / 钱包 / 提现
 */
@Tag(name = "结算模块")
@RestController
@RequestMapping("/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @Operation(summary = "我的结算记录")
    @GetMapping
    public Result<PageResult<SettlementVO>> mySettlements(@RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(settlementService.mySettlements(UserContext.userId(), page, size));
    }

    @Operation(summary = "钱包信息（余额/冻结/收支统计）")
    @GetMapping("/wallet")
    public Result<WalletVO> wallet() {
        return Result.ok(settlementService.wallet(UserContext.userId()));
    }

    @Operation(summary = "申请提现")
    @PostMapping("/withdrawals")
    public Result<Void> withdraw(@Validated @RequestBody WithdrawDTO dto) {
        settlementService.withdraw(UserContext.userId(), dto);
        return Result.ok();
    }

    @Operation(summary = "我的提现记录")
    @GetMapping("/withdrawals")
    public Result<PageResult<WithdrawalVO>> myWithdrawals(@RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(settlementService.myWithdrawals(UserContext.userId(), page, size));
    }
}
