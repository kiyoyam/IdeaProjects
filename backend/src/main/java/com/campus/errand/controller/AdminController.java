package com.campus.errand.controller;

import com.campus.errand.common.PageResult;
import com.campus.errand.common.Result;
import com.campus.errand.dto.RejectDTO;
import com.campus.errand.dto.TaskQueryDTO;
import com.campus.errand.service.SettlementService;
import com.campus.errand.service.TaskService;
import com.campus.errand.service.UserService;
import com.campus.errand.vo.SettlementVO;
import com.campus.errand.vo.TaskVO;
import com.campus.errand.vo.UserVO;
import com.campus.errand.vo.WithdrawalVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台接口（仅 ADMIN 角色可访问，由 AdminInterceptor 校验）
 */
@Tag(name = "管理后台")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final TaskService taskService;
    private final SettlementService settlementService;

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public Result<PageResult<UserVO>> users(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) String keyword) {
        return Result.ok(userService.pageUsers(page, size, keyword));
    }

    @Operation(summary = "任务列表")
    @GetMapping("/tasks")
    public Result<PageResult<TaskVO>> tasks(TaskQueryDTO dto) {
        return Result.ok(taskService.pageQuery(dto, null));
    }

    @Operation(summary = "结算记录")
    @GetMapping("/settlements")
    public Result<PageResult<SettlementVO>> settlements(@RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "10") Integer size,
                                                        @RequestParam(required = false) String type) {
        return Result.ok(settlementService.adminSettlements(page, size, type));
    }

    @Operation(summary = "提现申请列表")
    @GetMapping("/withdrawals")
    public Result<PageResult<WithdrawalVO>> withdrawals(@RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "10") Integer size,
                                                        @RequestParam(required = false) String status) {
        return Result.ok(settlementService.adminWithdrawals(page, size, status));
    }

    @Operation(summary = "审批通过提现")
    @PostMapping("/withdrawals/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        settlementService.approveWithdrawal(id);
        return Result.ok();
    }

    @Operation(summary = "驳回提现（余额退回）")
    @PostMapping("/withdrawals/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) RejectDTO dto) {
        settlementService.rejectWithdrawal(id, dto == null ? null : dto.getRemark());
        return Result.ok();
    }
}
