package com.campus.errand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.errand.common.BusinessException;
import com.campus.errand.common.Constants;
import com.campus.errand.common.PageResult;
import com.campus.errand.dto.TaskPublishDTO;
import com.campus.errand.dto.TaskQueryDTO;
import com.campus.errand.entity.ErrandOrder;
import com.campus.errand.entity.Evaluation;
import com.campus.errand.entity.Task;
import com.campus.errand.entity.User;
import com.campus.errand.mapper.ErrandOrderMapper;
import com.campus.errand.mapper.EvaluationMapper;
import com.campus.errand.mapper.TaskMapper;
import com.campus.errand.mapper.UserMapper;
import com.campus.errand.service.SettlementService;
import com.campus.errand.service.TaskService;
import com.campus.errand.service.UserService;
import com.campus.errand.vo.OrderVO;
import com.campus.errand.vo.TaskOverviewVO;
import com.campus.errand.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 任务服务实现
 */
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    private final ErrandOrderMapper errandOrderMapper;
    private final EvaluationMapper evaluationMapper;
    private final UserService userService;
    private final SettlementService settlementService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskVO publish(TaskPublishDTO dto, Long userId) {
        BigDecimal reward = dto.getReward();
        if (reward.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("报酬金额必须大于0");
        }
        User user = userService.getByIdRequired(userId);
        if (user.getBalance().compareTo(reward) < 0) {
            throw new BusinessException("余额不足，请先充值后再发布任务");
        }
        // 原子扣减可用余额并冻结
        int updated = userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .ge(User::getBalance, reward)
                .setSql("balance = balance - " + reward.toPlainString())
                .setSql("frozen_balance = frozen_balance + " + reward.toPlainString()));
        if (updated == 0) {
            throw new BusinessException("余额不足，请先充值后再发布任务");
        }
        Task task = new Task();
        task.setPublisherId(userId);
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setCategory(StringUtils.hasText(dto.getCategory()) ? dto.getCategory() : "OTHER");
        task.setReward(reward);
        task.setPickupAddress(dto.getPickupAddress());
        task.setDeliveryAddress(dto.getDeliveryAddress());
        task.setDeadline(dto.getDeadline());
        task.setStatus(Constants.STATUS_PENDING);
        taskMapper.insert(task);
        return toVO(task, userId);
    }

    @Override
    public PageResult<TaskVO> pageQuery(TaskQueryDTO dto, Long userId) {
        Page<Task> page = new Page<>(dto.getPage() == null ? 1 : dto.getPage(), dto.getSize() == null ? 10 : dto.getSize());
        LambdaQueryWrapper<Task> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getKeyword())) {
            String kw = dto.getKeyword().trim();
            qw.and(w -> w.like(Task::getTitle, kw).or().like(Task::getDescription, kw));
        }
        if (StringUtils.hasText(dto.getCategory())) {
            qw.eq(Task::getCategory, dto.getCategory());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            qw.eq(Task::getStatus, dto.getStatus());
        }
        // 排序：默认最新，支持按报酬、截止时间
        String sort = dto.getSort();
        if ("reward".equalsIgnoreCase(sort)) {
            qw.orderByDesc(Task::getReward).orderByDesc(Task::getCreateTime);
        } else if ("deadline".equalsIgnoreCase(sort)) {
            qw.orderByAsc(Task::getDeadline).orderByDesc(Task::getCreateTime);
        } else {
            qw.orderByDesc(Task::getCreateTime);
        }
        taskMapper.selectPage(page, qw);
        List<TaskVO> vos = page.getRecords().stream().map(t -> toVO(t, userId)).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), vos);
    }

    @Override
    public TaskVO detail(Long taskId, Long userId) {
        Task task = getTaskRequired(taskId);
        TaskVO vo = toVO(task, userId);
        // 详情页需要展示「接单时间」，取该任务最近一条接单记录
        ErrandOrder order = errandOrderMapper.selectOne(new LambdaQueryWrapper<ErrandOrder>()
                .eq(ErrandOrder::getTaskId, taskId)
                .orderByDesc(ErrandOrder::getId)
                .last("limit 1"));
        if (order != null) {
            vo.setAcceptTime(order.getCreateTime());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long taskId, Long userId) {
        Task task = getTaskRequired(taskId);
        if (task.getPublisherId().equals(userId)) {
            throw new BusinessException("不能接自己发布的任务");
        }
        if (!Constants.STATUS_PENDING.equals(task.getStatus())) {
            throw new BusinessException("任务当前不可接单");
        }
        // 条件更新防止并发重复接单
        int updated = taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                .eq(Task::getId, taskId)
                .eq(Task::getStatus, Constants.STATUS_PENDING)
                .isNull(Task::getRunnerId)
                .set(Task::getRunnerId, userId)
                .set(Task::getStatus, Constants.STATUS_ACCEPTED));
        if (updated == 0) {
            throw new BusinessException("手慢了，任务已被其他人接单");
        }
        ErrandOrder order = new ErrandOrder();
        order.setTaskId(taskId);
        order.setPublisherId(task.getPublisherId());
        order.setRunnerId(userId);
        order.setReward(task.getReward());
        order.setStatus(Constants.STATUS_ACCEPTED);
        errandOrderMapper.insert(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void start(Long taskId, Long userId) {
        transition(taskId, userId, Constants.STATUS_ACCEPTED, Constants.STATUS_IN_PROGRESS, "仅已接单状态可开始配送");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(Long taskId, Long userId) {
        transition(taskId, userId, Constants.STATUS_IN_PROGRESS, Constants.STATUS_FINISHED, "仅进行中状态可标记送达");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long taskId, Long userId) {
        Task task = getTaskRequired(taskId);
        if (!task.getPublisherId().equals(userId)) {
            throw new BusinessException("仅发布者可确认完成");
        }
        if (!Constants.STATUS_FINISHED.equals(task.getStatus())) {
            throw new BusinessException("任务尚未标记送达，无法确认完成");
        }
        int updated = taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                .eq(Task::getId, taskId)
                .eq(Task::getStatus, Constants.STATUS_FINISHED)
                .set(Task::getStatus, Constants.STATUS_COMPLETED)
                .set(Task::getCompleteTime, LocalDateTime.now()));
        if (updated == 0) {
            throw new BusinessException("任务状态已变更，请刷新后重试");
        }
        // 同步接单记录状态
        errandOrderMapper.update(null, new LambdaUpdateWrapper<ErrandOrder>()
                .eq(ErrandOrder::getTaskId, taskId)
                .set(ErrandOrder::getStatus, Constants.STATUS_COMPLETED)
                .set(ErrandOrder::getCompleteTime, LocalDateTime.now()));
        // 触发结算：冻结余额转入跑腿员
        task.setStatus(Constants.STATUS_COMPLETED);
        settlementService.settle(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long taskId, Long userId, String reason) {
        Task task = getTaskRequired(taskId);
        boolean isPublisher = task.getPublisherId().equals(userId);
        boolean isRunner = userId.equals(task.getRunnerId());
        if (!isPublisher && !isRunner) {
            throw new BusinessException("仅发布者或接单跑腿员可取消任务");
        }
        String status = task.getStatus();
        if (Constants.STATUS_COMPLETED.equals(status) || Constants.STATUS_CANCELLED.equals(status)
                || Constants.STATUS_FINISHED.equals(status)) {
            throw new BusinessException("当前状态不可取消");
        }
        if (isRunner && Constants.STATUS_PENDING.equals(status)) {
            throw new BusinessException("尚未接单，无需取消");
        }
        int updated = taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                .eq(Task::getId, taskId)
                .in(Task::getStatus, Constants.STATUS_PENDING, Constants.STATUS_ACCEPTED, Constants.STATUS_IN_PROGRESS)
                .set(Task::getStatus, Constants.STATUS_CANCELLED)
                .set(Task::getCancelReason, reason));
        if (updated == 0) {
            throw new BusinessException("任务状态已变更，请刷新后重试");
        }
        // 同步接单记录状态
        errandOrderMapper.update(null, new LambdaUpdateWrapper<ErrandOrder>()
                .eq(ErrandOrder::getTaskId, taskId)
                .and(w -> w.eq(ErrandOrder::getStatus, Constants.STATUS_ACCEPTED)
                        .or().eq(ErrandOrder::getStatus, Constants.STATUS_IN_PROGRESS))
                .set(ErrandOrder::getStatus, Constants.STATUS_CANCELLED));
        // 触发退款：冻结余额退回发布者
        settlementService.refund(task, reason);
    }

    @Override
    public PageResult<TaskVO> myPublished(Long userId, Integer page, Integer size) {
        Page<Task> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        taskMapper.selectPage(p, new LambdaQueryWrapper<Task>()
                .eq(Task::getPublisherId, userId)
                .orderByDesc(Task::getCreateTime));
        List<TaskVO> vos = p.getRecords().stream().map(t -> toVO(t, userId)).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), vos);
    }

    @Override
    public PageResult<OrderVO> myAccepted(Long userId, Integer page, Integer size, String status) {
        Page<ErrandOrder> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        LambdaQueryWrapper<ErrandOrder> qw = new LambdaQueryWrapper<ErrandOrder>()
                .eq(ErrandOrder::getRunnerId, userId);
        if (StringUtils.hasText(status)) {
            if ("ACTIVE".equalsIgnoreCase(status)) {
                // 进行中 = 已接单 + 配送中
                qw.in(ErrandOrder::getStatus, Constants.STATUS_ACCEPTED, Constants.STATUS_IN_PROGRESS);
            } else {
                qw.eq(ErrandOrder::getStatus, status);
            }
        }
        qw.orderByDesc(ErrandOrder::getCreateTime);
        errandOrderMapper.selectPage(p, qw);
        List<ErrandOrder> orders = p.getRecords();
        Set<Long> taskIds = orders.stream().map(ErrandOrder::getTaskId).collect(Collectors.toSet());
        Map<Long, Task> taskMap = taskIds.isEmpty() ? Collections.emptyMap()
                : taskMapper.selectBatchIds(taskIds).stream().collect(Collectors.toMap(Task::getId, Function.identity()));
        Set<Long> publisherIds = orders.stream().map(ErrandOrder::getPublisherId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMap(publisherIds);
        List<OrderVO> vos = new ArrayList<>();
        for (ErrandOrder o : orders) {
            OrderVO vo = new OrderVO();
            vo.setId(o.getId());
            vo.setTaskId(o.getTaskId());
            vo.setReward(o.getReward());
            vo.setStatus(o.getStatus());
            vo.setPublisherId(o.getPublisherId());
            User publisher = userMap.get(o.getPublisherId());
            vo.setPublisherName(publisher == null ? null : displayName(publisher));
            vo.setCreateTime(o.getCreateTime());
            vo.setCompleteTime(o.getCompleteTime());
            Task t = taskMap.get(o.getTaskId());
            if (t != null) {
                vo.setTitle(t.getTitle());
                vo.setCategory(t.getCategory());
                vo.setPickupAddress(t.getPickupAddress());
                vo.setDeliveryAddress(t.getDeliveryAddress());
                vo.setDeadline(t.getDeadline());
            }
            vos.add(vo);
        }
        return new PageResult<>(p.getTotal(), vos);
    }

    @Override
    public TaskOverviewVO overview(Long userId) {
        TaskOverviewVO vo = new TaskOverviewVO();
        // 我作为跑腿员的接单情况
        Long active = errandOrderMapper.selectCount(new LambdaQueryWrapper<ErrandOrder>()
                .eq(ErrandOrder::getRunnerId, userId)
                .in(ErrandOrder::getStatus, Constants.STATUS_ACCEPTED, Constants.STATUS_IN_PROGRESS));
        Long completed = errandOrderMapper.selectCount(new LambdaQueryWrapper<ErrandOrder>()
                .eq(ErrandOrder::getRunnerId, userId)
                .eq(ErrandOrder::getStatus, Constants.STATUS_COMPLETED));
        Long cancelled = errandOrderMapper.selectCount(new LambdaQueryWrapper<ErrandOrder>()
                .eq(ErrandOrder::getRunnerId, userId)
                .eq(ErrandOrder::getStatus, Constants.STATUS_CANCELLED));
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Long today = errandOrderMapper.selectCount(new LambdaQueryWrapper<ErrandOrder>()
                .eq(ErrandOrder::getRunnerId, userId)
                .ge(ErrandOrder::getCreateTime, todayStart));
        vo.setActiveCount(active);
        vo.setCompletedCount(completed);
        vo.setCancelledCount(cancelled);
        vo.setTodayCount(today);
        // 累计收入 = 已完成订单报酬之和
        List<ErrandOrder> completedOrders = errandOrderMapper.selectList(new LambdaQueryWrapper<ErrandOrder>()
                .eq(ErrandOrder::getRunnerId, userId)
                .eq(ErrandOrder::getStatus, Constants.STATUS_COMPLETED));
        vo.setIncome(completedOrders.stream()
                .map(o -> o.getReward() == null ? BigDecimal.ZERO : o.getReward())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        // 评分
        User user = userService.getByIdRequired(userId);
        vo.setRating(user.getRating() == null ? BigDecimal.ZERO : user.getRating());
        vo.setRatingCount(user.getRatingCount() == null ? 0 : user.getRatingCount());
        // 我作为发布者的任务情况
        vo.setPublishedTotal(taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getPublisherId, userId)));
        vo.setPublishedActive(taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getPublisherId, userId)
                .in(Task::getStatus, Constants.STATUS_PENDING, Constants.STATUS_ACCEPTED,
                        Constants.STATUS_IN_PROGRESS, Constants.STATUS_FINISHED)));
        // 广场概览
        vo.setPendingCount(taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getStatus, Constants.STATUS_PENDING)));
        vo.setTodayCompleted(taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getStatus, Constants.STATUS_COMPLETED)
                .ge(Task::getCompleteTime, todayStart)));
        return vo;
    }

    // ==================== 私有方法 ====================

    private Task getTaskRequired(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        return task;
    }

    /** 跑腿员操作的任务状态流转（任务 + 接单记录同步更新） */
    private void transition(Long taskId, Long userId, String fromStatus, String toStatus, String errMsg) {
        Task task = getTaskRequired(taskId);
        if (!userId.equals(task.getRunnerId())) {
            throw new BusinessException("仅接单跑腿员可操作");
        }
        int updated = taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                .eq(Task::getId, taskId)
                .eq(Task::getStatus, fromStatus)
                .set(Task::getStatus, toStatus));
        if (updated == 0) {
            throw new BusinessException(errMsg);
        }
        errandOrderMapper.update(null, new LambdaUpdateWrapper<ErrandOrder>()
                .eq(ErrandOrder::getTaskId, taskId)
                .eq(ErrandOrder::getStatus, fromStatus)
                .set(ErrandOrder::getStatus, toStatus));
    }

    private TaskVO toVO(Task task, Long userId) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setPublisherId(task.getPublisherId());
        vo.setRunnerId(task.getRunnerId());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setCategory(task.getCategory());
        vo.setReward(task.getReward());
        vo.setPickupAddress(task.getPickupAddress());
        vo.setDeliveryAddress(task.getDeliveryAddress());
        vo.setDeadline(task.getDeadline());
        vo.setStatus(task.getStatus());
        vo.setCancelReason(task.getCancelReason());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        vo.setCompleteTime(task.getCompleteTime());
        // 双方用户信息
        Set<Long> ids = new HashSet<>();
        if (task.getPublisherId() != null) ids.add(task.getPublisherId());
        if (task.getRunnerId() != null) ids.add(task.getRunnerId());
        Map<Long, User> userMap = userMap(ids);
        User publisher = userMap.get(task.getPublisherId());
        if (publisher != null) {
            vo.setPublisherName(displayName(publisher));
            vo.setPublisherRating(publisher.getRating());
        }
        User runner = userMap.get(task.getRunnerId());
        if (runner != null) {
            vo.setRunnerName(displayName(runner));
            vo.setRunnerRating(runner.getRating());
        }
        // 当前用户身份
        if (userId != null) {
            if (userId.equals(task.getPublisherId())) {
                vo.setMyRole("PUBLISHER");
            } else if (userId.equals(task.getRunnerId())) {
                vo.setMyRole("RUNNER");
            } else {
                vo.setMyRole("NONE");
            }
            if (Constants.STATUS_COMPLETED.equals(task.getStatus())) {
                Long count = evaluationMapper.selectCount(new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getTaskId, task.getId())
                        .eq(Evaluation::getEvaluatorId, userId));
                vo.setEvaluated(count != null && count > 0);
            }
        } else {
            vo.setMyRole("NONE");
        }
        return vo;
    }

    private Map<Long, User> userMap(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(ids);
        return users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private String displayName(User user) {
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }
}
