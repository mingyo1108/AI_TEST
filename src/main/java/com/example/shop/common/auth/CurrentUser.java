package com.example.shop.common.auth;

import com.example.shop.common.BusinessException;
import com.example.shop.common.ErrorCode;
import java.util.Set;

/**
 * 요청한 사용자. 사내 SSO 게이트웨이가 넣어 주는 헤더({@code X-User-Id}, {@code X-User-Roles})에서 만든다.
 *
 * @param userId 사용자 ID (사번)
 * @param roles 역할
 */
public record CurrentUser(String userId, Set<Role> roles) {

    /** 배치가 처리할 때 기록하는 사용자 ID. */
    public static final String SYSTEM_USER_ID = "SYSTEM";

    /** 역할이 없으면 403으로 막는다. 컨트롤러 메서드의 첫 줄에서 호출한다. */
    public void requireRole(Role role) {
        if (!roles.contains(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, role.name() + " 역할 필요");
        }
    }
}
