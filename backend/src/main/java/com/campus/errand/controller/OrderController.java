package com.campus.errand.controller;

import com.campus.errand.common.PageResult;
import com.campus.errand.common.Result;
import com.campus.errand.security.UserContext;
import com.campus.errand.service.TaskService;
import com.campus.errand.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 接单记录接口（跑腿员视角）
 */
@Tag(name = "接单模块")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final TaskService taskService;

    @Operation(summary = "我的接单记录")
    @GetMapping
    public Result<PageResult<OrderVO>> myOrders(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(taskService.myAccepted(UserContext.userId(), page, size));
    }
}
