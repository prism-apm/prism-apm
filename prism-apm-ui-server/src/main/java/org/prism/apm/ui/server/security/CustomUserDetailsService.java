package org.prism.apm.ui.server.security;

import org.prism.apm.ui.server.entity.dto.security.UserDTO;
import org.prism.apm.ui.server.entity.model.security.User;
import org.prism.apm.ui.server.service.security.UserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public CustomUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.findByUsername(username);
        if (user == null || user.getId() == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        // Get user roles
        List<String> role_names = userService.getUserRoleNames(user.getId());

        return new UserDTO(
                user.getId(),             // New first parameter
                role_names,
                user.getUsername(),
                user.getPassword(),
                user.getEnabled(),
                true,                         // accountNonExpired
                true,                         // credentialsNonExpired
                true,                         // accountNonLocked
                role_names.stream()
                        .map(roleName -> new SimpleGrantedAuthority("ROLE_" + roleName))
                        .collect(Collectors.toList()));
    }
} 