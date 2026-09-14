package org.prism.apm.ui.server.utils;

import org.prism.apm.ui.server.constant.security.RoleConstants;
import org.prism.apm.ui.server.entity.dto.security.UserDTO;
import lombok.SneakyThrows;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.security.auth.login.AccountExpiredException;

public class SecurityUtils {

    @SneakyThrows
    public static UserDTO getCurrentUser() {
        // 1. Get SecurityContext from SecurityContextHolder
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. Check whether the authentication object is valid
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // "anonymousUser" is the default Principal set by Spring Security for unauthenticated users
            throw new AccountExpiredException("User is not logged in");
        }

        // 3. Get Principal
        Object principal = authentication.getPrincipal();

        // 4. Type conversion
        return (UserDTO) principal;
    }

    public static boolean isCurrentUserAdmin(UserDTO user){
        return user.getRoleName().contains(RoleConstants.ROLE_ADMIN_DB);
    }

}
