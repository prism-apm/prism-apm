package org.prism.apm.ui.server.entity.dto.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

public class UserDTO extends org.springframework.security.core.userdetails.User {

    @Getter
    private final Long userId;
    @Getter
    private final List<String> roleName;

    /**
     * Custom constructor used to create a complete UserDTO object at once
     */
    public UserDTO(Long userId, List<String> roleName, String username, String password, boolean enabled,
                   boolean accountNonExpired, boolean credentialsNonExpired,
                   boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities) {
        // Call the superclass constructor first
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        // Then initialize its own fields
        this.userId = userId;
        this.roleName = roleName;
    }

}