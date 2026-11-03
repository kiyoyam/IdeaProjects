package com.campus.errand.controller;

import com.campus.errand.common.Result;
import com.campus.errand.dto.LoginDTO;
import com.campus.errand.dto.RechargeDTO;
import com.campus.errand.dto.RegisterDTO;
import com.campus.errand.security.UserContext;
import com.campus.errand.service.UserService;
import com.campus.errand.vo.LoginVO;
import com.campus.errand.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证与用户接口
 */
@Tag(name = "认证与用户")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "注册（STUDENT/RUNNER）")
    @PostMapping("/register")
    public Result<LoginVO> register(@Validated @RequestBody RegisterDTO dto) {
        return Result.ok(userService.register(dto));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        return Result.ok(userService.login(dto));
    }

    @Operation(summary = "当前登录用户信息")
    @GetMapping("/info")
    public Result<UserVO> info() {
        return Result.ok(userService.currentUser());
    }

    @Operation(summary = "模拟充值")
    @PostMapping("/recharge")
    public Result<UserVO> recharge(@Validated @RequestBody RechargeDTO dto) {
        return Result.ok(userService.recharge(UserContext.userId(), dto));
    }
}
