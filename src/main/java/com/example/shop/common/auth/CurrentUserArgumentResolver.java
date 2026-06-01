package com.example.shop.common.auth;

import com.example.shop.common.BusinessException;
import com.example.shop.common.ErrorCode;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 헤더에서 {@link CurrentUser}를 만든다. 사용자 ID 헤더가 없으면 401.
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    /** 사용자 ID 헤더. */
    public static final String USER_ID_HEADER = "X-User-Id";
    /** 역할 헤더 (쉼표로 구분). */
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CurrentUser.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String userId = webRequest.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return new CurrentUser(userId.trim(), parseRoles(webRequest.getHeader(USER_ROLES_HEADER)));
    }

    private Set<Role> parseRoles(String header) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        if (header == null || header.isBlank()) {
            return roles;
        }
        Arrays.stream(header.split(","))
                .map(String::trim)
                .forEach(value -> Arrays.stream(Role.values())
                        .filter(role -> role.name().equalsIgnoreCase(value))
                        .forEach(roles::add));
        return roles;
    }
}
