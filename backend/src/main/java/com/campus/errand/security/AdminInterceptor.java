package com.campus.errand.security;

import com.campus.errand.common.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 管理员权限拦截器（需在 AuthInterceptor 之后执行）
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"ADMIN".equals(UserContext.role())) {
            throw new BusinessException(403, "无权限访问，仅管理员可操作");
        }
        return true;
    }
}
