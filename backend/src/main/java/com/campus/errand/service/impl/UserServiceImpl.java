package com.campus.errand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.errand.common.BusinessException;
import com.campus.errand.common.Constants;
import com.campus.errand.common.PageResult;
import com.campus.errand.dto.LoginDTO;
import com.campus.errand.dto.RechargeDTO;
import com.campus.errand.dto.RegisterDTO;
import com.campus.errand.entity.User;
import com.campus.errand.mapper.UserMapper;
import com.campus.errand.security.JwtUtil;
import com.campus.errand.security.UserContext;
import com.campus.errand.service.UserService;
import com.campus.errand.vo.LoginVO;
import com.campus.errand.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginVO register(RegisterDTO dto) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setRole(StringUtils.hasText(dto.getRole()) ? dto.getRole() : Constants.ROLE_STUDENT);
        user.setBalance(BigDecimal.ZERO);
        user.setFrozenBalance(BigDecimal.ZERO);
        user.setRating(new BigDecimal("5.00"));
        user.setRatingCount(0);
        user.setStatus(1);
        userMapper.insert(user);
        return buildLoginVO(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        return buildLoginVO(user);
    }

    @Override
    public UserVO currentUser() {
        return toVO(getByIdRequired(UserContext.userId()));
    }

    @Override
    public UserVO recharge(Long userId, RechargeDTO dto) {
        getByIdRequired(userId);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("balance = balance + " + dto.getAmount().toPlainString()));
        return toVO(getByIdRequired(userId));
    }

    @Override
    public User getByIdRequired(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    @Override
    public UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setBalance(user.getBalance());
        vo.setFrozenBalance(user.getFrozenBalance());
        vo.setRating(user.getRating());
        vo.setRatingCount(user.getRatingCount());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    @Override
    public PageResult<UserVO> pageUsers(Integer page, Integer size, String keyword) {
        Page<User> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
        }
        qw.orderByDesc(User::getCreateTime);
        userMapper.selectPage(p, qw);
        List<UserVO> vos = p.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), vos);
    }

    private LoginVO buildLoginVO(User user) {
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, toVO(user));
    }
}
