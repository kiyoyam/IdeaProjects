package com.campus.errand.controller;

import com.campus.errand.common.PageResult;
import com.campus.errand.common.Result;
import com.campus.errand.dto.CancelDTO;
import com.campus.errand.dto.TaskPublishDTO;
import com.campus.errand.dto.TaskQueryDTO;
import com.campus.errand.security.UserContext;
import com.campus.errand.service.TaskService;
import com.campus.errand.vo.OrderVO;
import com.campus.errand.vo.TaskOverviewVO;
import com.campus.errand.vo.TaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务接口：发布 / 接单 / 配送 / 完成 / 取消
 */
@Tag(name = "任务模块")
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "发布任务（发布时冻结报酬金额）")
    @PostMapping
    public Result<TaskVO> publish(@Validated @RequestBody TaskPublishDTO dto) {
        return Result.ok(taskService.publish(dto, UserContext.userId()));
    }

    @Operation(summary = "任务广场分页查询")
    @GetMapping
    public Result<PageResult<TaskVO>> page(TaskQueryDTO dto) {
        return Result.ok(taskService.pageQuery(dto, UserContext.userId()));
    }

    @Operation(summary = "任务详情")
    @GetMapping("/{id}")
    public Result<TaskVO> detail(@PathVariable Long id) {
        return Result.ok(taskService.detail(id, UserContext.userId()));
    }

    @Operation(summary = "接单")
    @PostMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id) {
        taskService.accept(id, UserContext.userId());
        return Result.ok();
    }

    @Operation(summary = "开始配送")
    @PostMapping("/{id}/start")
    public Result<Void> start(@PathVariable Long id) {
        taskService.start(id, UserContext.userId());
        return Result.ok();
    }

    @Operation(summary = "标记送达（等待发布者确认）")
    @PostMapping("/{id}/finish")
    public Result<Void> finish(@PathVariable Long id) {
        taskService.finish(id, UserContext.userId());
        return Result.ok();
    }

    @Operation(summary = "发布者确认完成（触发结算）")
    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        taskService.confirm(id, UserContext.userId());
        return Result.ok();
    }

    @Operation(summary = "取消任务（发布者/跑腿员，触发退款）")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody(required = false) CancelDTO dto) {
        String reason = dto == null ? null : dto.getReason();
        taskService.cancel(id, UserContext.userId(), reason);
        return Result.ok();
    }

    @Operation(summary = "我发布的任务")
    @GetMapping("/mine/published")
    public Result<PageResult<TaskVO>> myPublished(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(taskService.myPublished(UserContext.userId(), page, size));
    }

    @Operation(summary = "我接的订单")
    @GetMapping("/mine/accepted")
    public Result<PageResult<OrderVO>> myAccepted(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer size,
                                                  @RequestParam(required = false) String status) {
        return Result.ok(taskService.myAccepted(UserContext.userId(), page, size, status));
    }

    @Operation(summary = "工作台统计（接单数/收入/评分/广场概览）")
    @GetMapping("/overview")
    public Result<TaskOverviewVO> overview() {
        return Result.ok(taskService.overview(UserContext.userId()));
    }
}
