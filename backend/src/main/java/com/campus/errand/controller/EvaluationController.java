package com.campus.errand.controller;

import com.campus.errand.common.PageResult;
import com.campus.errand.common.Result;
import com.campus.errand.dto.EvaluateDTO;
import com.campus.errand.security.UserContext;
import com.campus.errand.service.EvaluationService;
import com.campus.errand.vo.EvaluationPendingVO;
import com.campus.errand.vo.EvaluationSummaryVO;
import com.campus.errand.vo.EvaluationVO;
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

import java.util.List;

/**
 * 评价接口
 */
@Tag(name = "评价模块")
@RestController
@RequestMapping("/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @Operation(summary = "对已完成任务进行评价（评分模板：总分+三维度+标签+匿名）")
    @PostMapping
    public Result<EvaluationVO> evaluate(@Validated @RequestBody EvaluateDTO dto) {
        return Result.ok(evaluationService.evaluate(UserContext.userId(), dto));
    }

    @Operation(summary = "查询某任务的评价列表")
    @GetMapping("/task/{taskId}")
    public Result<List<EvaluationVO>> listByTask(@PathVariable Long taskId) {
        return Result.ok(evaluationService.listByTask(taskId));
    }

    @Operation(summary = "待评价订单（我参与且已完成、尚未评价）")
    @GetMapping("/pending")
    public Result<PageResult<EvaluationPendingVO>> pending(@RequestParam(defaultValue = "1") Integer page,
                                                           @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(evaluationService.pending(UserContext.userId(), page, size));
    }

    @Operation(summary = "我收到的评价")
    @GetMapping("/received")
    public Result<PageResult<EvaluationVO>> received(@RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(evaluationService.received(UserContext.userId(), page, size));
    }

    @Operation(summary = "我发出的评价")
    @GetMapping("/sent")
    public Result<PageResult<EvaluationVO>> sent(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(evaluationService.sent(UserContext.userId(), page, size));
    }

    @Operation(summary = "我的评价汇总")
    @GetMapping("/summary/me")
    public Result<EvaluationSummaryVO> mySummary() {
        return Result.ok(evaluationService.summary(UserContext.userId()));
    }

    @Operation(summary = "指定用户的评价汇总")
    @GetMapping("/summary/{userId}")
    public Result<EvaluationSummaryVO> summary(@PathVariable Long userId) {
        return Result.ok(evaluationService.summary(userId));
    }
}
