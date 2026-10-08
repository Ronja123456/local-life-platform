package com.hmdp.interceptor;

import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 判断 ThreadLocal 里有没有用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 没登录，返回 401
            response.setStatus(401);
            return false;
        }
        return true;
    }
}