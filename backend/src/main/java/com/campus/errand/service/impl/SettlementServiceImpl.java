package com.campus.errand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.errand.common.BusinessException;
import com.campus.errand.common.Constants;
import com.campus.errand.common.PageResult;
import com.campus.errand.dto.WithdrawDTO;
import com.campus.errand.entity.ErrandOrder;
import com.campus.errand.entity.Settlement;
import com.campus.errand.entity.Task;
import com.campus.errand.entity.User;
import com.campus.errand.entity.Withdrawal;
import com.campus.errand.mapper.ErrandOrderMapper;
import com.campus.errand.mapper.SettlementMapper;
import com.campus.errand.mapper.TaskMapper;
import com.campus.errand.mapper.UserMapper;
import com.campus.errand.mapper.WithdrawalMapper;
import com.campus.errand.service.SettlementService;
import com.campus.errand.service.UserService;
import com.campus.errand.vo.SettlementVO;
import com.campus.errand.vo.WalletVO;
import com.campus.errand.vo.WithdrawalVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 结算服务实现
 */
@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    private final SettlementMapper settlementMapper;
    private final WithdrawalMapper withdrawalMapper;
    private final UserMapper userMapper;
    private final TaskMapper taskMapper;
    private final ErrandOrderMapper errandOrderMapper;
    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settle(Task task) {
        Long publisherId = task.getPublisherId();
        Long runnerId = task.getRunnerId();
        if (publisherId == null || runnerId == null) {
            throw new BusinessException("任务缺少结算参与人");
        }
        BigDecimal reward = task.getReward();
        // 发布者冻结余额清零
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, publisherId)
                .ge(User::getFrozenBalance, reward)
                .setSql("frozen_balance = frozen_balance - " + reward.toPlainString()));
        // 跑腿员余额增加
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, runnerId)
                .setSql("balance = balance + " + reward.toPlainString()));
        ErrandOrder order = errandOrderMapper.selectOne(new LambdaQueryWrapper<ErrandOrder>()
                .eq(ErrandOrder::getTaskId, task.getId()));
        Settlement s = new Settlement();
        s.setTaskId(task.getId());
        s.setOrderId(order == null ? null : order.getId());
        s.setPublisherId(publisherId);
        s.setRunnerId(runnerId);
        s.setAmount(reward);
        s.setType(Constants.SETTLE_TASK);
        s.setStatus("SUCCESS");
        s.setRemark("任务完成，报酬结算给跑腿员");
        settlementMapper.insert(s);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Task task, String reason) {
        BigDecimal reward = task.getReward();
        // 冻结余额退回发布者
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, task.getPublisherId())
                .ge(User::getFrozenBalance, reward)
                .setSql("frozen_balance = frozen_balance - " + reward.toPlainString())
                .setSql("balance = balance + " + reward.toPlainString()));
        Settlement s = new Settlement();
        s.setTaskId(task.getId());
        s.setPublisherId(task.getPublisherId());
        s.setRunnerId(task.getRunnerId());
        s.setAmount(reward);
        s.setType(Constants.SETTLE_REFUND);
        s.setStatus("SUCCESS");
        s.setRemark(StringUtils.hasText(reason) ? "任务取消退款：" + reason : "任务取消，冻结金额退回");
        settlementMapper.insert(s);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long userId, WithdrawDTO dto) {
        userService.getByIdRequired(userId);
        // 原子扣减余额
        int updated = userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .ge(User::getBalance, dto.getAmount())
                .setSql("balance = balance - " + dto.getAmount().toPlainString()));
        if (updated == 0) {
            throw new BusinessException("余额不足，无法提现");
        }
        Withdrawal w = new Withdrawal();
        w.setUserId(userId);
        w.setAmount(dto.getAmount());
        w.setStatus(Constants.WITHDRAW_PENDING);
        withdrawalMapper.insert(w);
    }

    @Override
    public WalletVO wallet(Long userId) {
        User user = userService.getByIdRequired(userId);
        WalletVO vo = new WalletVO();
        vo.setBalance(user.getBalance());
        vo.setFrozenBalance(user.getFrozenBalance());
        // 累计收入：作为跑腿员的 TASK 结算
        List<Settlement> incomeList = settlementMapper.selectList(new LambdaQueryWrapper<Settlement>()
                .eq(Settlement::getRunnerId, userId)
                .eq(Settlement::getType, Constants.SETTLE_TASK));
        vo.setTotalIncome(sum(incomeList));
        // 累计支出：作为发布者的 TASK 结算 - 退款
        List<Settlement> expenseList = settlementMapper.selectList(new LambdaQueryWrapper<Settlement>()
                .eq(Settlement::getPublisherId, userId)
                .eq(Settlement::getType, Constants.SETTLE_TASK));
        List<Settlement> refundList = settlementMapper.selectList(new LambdaQueryWrapper<Settlement>()
                .eq(Settlement::getPublisherId, userId)
                .eq(Settlement::getType, Constants.SETTLE_REFUND));
        vo.setTotalExpense(sum(expenseList).subtract(sum(refundList)));
        // 累计提现
        List<Withdrawal> withdrawnList = withdrawalMapper.selectList(new LambdaQueryWrapper<Withdrawal>()
                .eq(Withdrawal::getUserId, userId)
                .eq(Withdrawal::getStatus, Constants.WITHDRAW_APPROVED));
        vo.setTotalWithdrawn(withdrawnList.stream()
                .map(Withdrawal::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        // 完成接单数
        Long completed = errandOrderMapper.selectCount(new LambdaQueryWrapper<ErrandOrder>()
                .eq(ErrandOrder::getRunnerId, userId)
                .eq(ErrandOrder::getStatus, Constants.STATUS_COMPLETED));
        vo.setCompletedOrders(completed);
        return vo;
    }

    @Override
    public PageResult<SettlementVO> mySettlements(Long userId, Integer page, Integer size) {
        Page<Settlement> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        settlementMapper.selectPage(p, new LambdaQueryWrapper<Settlement>()
                .and(w -> w.eq(Settlement::getPublisherId, userId).or().eq(Settlement::getRunnerId, userId))
                .orderByDesc(Settlement::getCreateTime));
        return new PageResult<>(p.getTotal(), toSettlementVOs(p.getRecords()));
    }

    @Override
    public PageResult<WithdrawalVO> myWithdrawals(Long userId, Integer page, Integer size) {
        Page<Withdrawal> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        withdrawalMapper.selectPage(p, new LambdaQueryWrapper<Withdrawal>()
                .eq(Withdrawal::getUserId, userId)
                .orderByDesc(Withdrawal::getCreateTime));
        return new PageResult<>(p.getTotal(), toWithdrawalVOs(p.getRecords()));
    }

    @Override
    public PageResult<SettlementVO> adminSettlements(Integer page, Integer size, String type) {
        Page<Settlement> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        LambdaQueryWrapper<Settlement> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(type)) {
            qw.eq(Settlement::getType, type);
        }
        qw.orderByDesc(Settlement::getCreateTime);
        settlementMapper.selectPage(p, qw);
        return new PageResult<>(p.getTotal(), toSettlementVOs(p.getRecords()));
    }

    @Override
    public PageResult<WithdrawalVO> adminWithdrawals(Integer page, Integer size, String status) {
        Page<Withdrawal> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        LambdaQueryWrapper<Withdrawal> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            qw.eq(Withdrawal::getStatus, status);
        }
        qw.orderByDesc(Withdrawal::getCreateTime);
        withdrawalMapper.selectPage(p, qw);
        return new PageResult<>(p.getTotal(), toWithdrawalVOs(p.getRecords()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveWithdrawal(Long id) {
        Withdrawal w = withdrawalMapper.selectById(id);
        if (w == null) {
            throw new BusinessException(404, "提现记录不存在");
        }
        int updated = withdrawalMapper.update(null, new LambdaUpdateWrapper<Withdrawal>()
                .eq(Withdrawal::getId, id)
                .eq(Withdrawal::getStatus, Constants.WITHDRAW_PENDING)
                .set(Withdrawal::getStatus, Constants.WITHDRAW_APPROVED)
                .set(Withdrawal::getHandleTime, LocalDateTime.now()));
        if (updated == 0) {
            throw new BusinessException("该提现已处理，请勿重复操作");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectWithdrawal(Long id, String remark) {
        Withdrawal w = withdrawalMapper.selectById(id);
        if (w == null) {
            throw new BusinessException(404, "提现记录不存在");
        }
        int updated = withdrawalMapper.update(null, new LambdaUpdateWrapper<Withdrawal>()
                .eq(Withdrawal::getId, id)
                .eq(Withdrawal::getStatus, Constants.WITHDRAW_PENDING)
                .set(Withdrawal::getStatus, Constants.WITHDRAW_REJECTED)
                .set(Withdrawal::getRemark, remark)
                .set(Withdrawal::getHandleTime, LocalDateTime.now()));
        if (updated == 0) {
            throw new BusinessException("该提现已处理，请勿重复操作");
        }
        // 驳回后余额退回
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, w.getUserId())
                .setSql("balance = balance + " + w.getAmount().toPlainString()));
    }

    // ==================== 私有方法 ====================

    private BigDecimal sum(List<Settlement> list) {
        return list.stream().map(Settlement::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<SettlementVO> toSettlementVOs(List<Settlement> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> taskIds = records.stream().map(Settlement::getTaskId).collect(Collectors.toSet());
        Map<Long, Task> taskMap = taskIds.isEmpty() ? Collections.emptyMap()
                : taskMapper.selectBatchIds(taskIds).stream().collect(Collectors.toMap(Task::getId, Function.identity()));
        Set<Long> userIds = new java.util.HashSet<>();
        records.forEach(s -> {
            userIds.add(s.getPublisherId());
            if (s.getRunnerId() != null) userIds.add(s.getRunnerId());
        });
        Map<Long, User> userMap = userMap(userIds);
        List<SettlementVO> vos = new ArrayList<>();
        for (Settlement s : records) {
            SettlementVO vo = new SettlementVO();
            vo.setId(s.getId());
            vo.setTaskId(s.getTaskId());
            vo.setPublisherId(s.getPublisherId());
            vo.setRunnerId(s.getRunnerId());
            vo.setAmount(s.getAmount());
            vo.setType(s.getType());
            vo.setStatus(s.getStatus());
            vo.setRemark(s.getRemark());
            vo.setCreateTime(s.getCreateTime());
            Task t = taskMap.get(s.getTaskId());
            if (t != null) vo.setTitle(t.getTitle());
            User publisher = userMap.get(s.getPublisherId());
            if (publisher != null) vo.setPublisherName(displayName(publisher));
            User runner = userMap.get(s.getRunnerId());
            if (runner != null) vo.setRunnerName(displayName(runner));
            vos.add(vo);
        }
        return vos;
    }

    private List<WithdrawalVO> toWithdrawalVOs(List<Withdrawal> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = records.stream().map(Withdrawal::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMap(userIds);
        List<WithdrawalVO> vos = new ArrayList<>();
        for (Withdrawal w : records) {
            WithdrawalVO vo = new WithdrawalVO();
            vo.setId(w.getId());
            vo.setUserId(w.getUserId());
            vo.setAmount(w.getAmount());
            vo.setStatus(w.getStatus());
            vo.setRemark(w.getRemark());
            vo.setCreateTime(w.getCreateTime());
            vo.setHandleTime(w.getHandleTime());
            User user = userMap.get(w.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
            }
            vos.add(vo);
        }
        return vos;
    }

    private Map<Long, User> userMap(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private String displayName(User user) {
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }
}
