package org.prism.apm.ui.server.security;

import org.prism.apm.ui.server.constant.security.RoleConstants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class CustomAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final DynamicPermissionService dynamicPermissionService;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier, RequestAuthorizationContext context) {
        HttpServletRequest request = context.getRequest();
        String requestUrl = request.getRequestURI(); // Note: Spring 6 usually matches URI

        // Get current authentication
        Authentication authentication = authenticationSupplier.get();

        // 1. *** Implement admin super permission ***
        // Check whether the user has ROLE_ADMIN
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(RoleConstants.ROLE_ADMIN::equals);

        if (isAdmin) {
            return new AuthorizationDecision(true);
        }

        // 2. Match URL rules
        Map<String, Collection<String>> permissionMap = dynamicPermissionService.getPermissionMap();

        for (Map.Entry<String, Collection<String>> entry : permissionMap.entrySet()) {
            String pattern = entry.getKey();

            // If the URL matches
            if (antPathMatcher.match(pattern, requestUrl)) {
                Collection<String> requiredAttributes = entry.getValue();

                // 3. Check whether the current user has the required permissions
                // Allow access if any configAttribute matches
                for (String requiredRole : requiredAttributes) {
                    for (GrantedAuthority authority : authentication.getAuthorities()) {
                        if (requiredRole.equals(authority.getAuthority())) {
                            return new AuthorizationDecision(true);
                        }
                    }
                }
                // URL matched, but no corresponding permission -> deny
                // Returning false here means explicit denial, corresponding to the original AccessDeniedException
                return new AuthorizationDecision(false);
            }
        }

        // 4. Default policy
        // If the URL is not in the configuration table, the previous logic is ROLE_DENY_ALL, meaning deny.
        // If unconfigured URLs should be allowed by default, change this to true.
        return new AuthorizationDecision(false);
    }
}
