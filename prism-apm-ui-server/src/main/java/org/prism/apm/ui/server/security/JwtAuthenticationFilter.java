package org.prism.apm.ui.server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtHelper jwtHelper;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtHelper jwtHelper, CustomUserDetailsService userDetailsService) {
        this.jwtHelper = jwtHelper;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        // 1. Get Header
        String authHeader = request.getHeader("Authorization");

        // 2. Check whether the format is "Bearer <token>"
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String username = jwtHelper.validateTokenAndGetUsername(token);

            // 3. Token is valid and SecurityContext is not authenticated yet
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 4. Load user details
                // Optimization tip: to avoid querying the database every time, put role information in the Token and build UserDetails directly from it
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 5. Build authentication object
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. Put it into the context -> "authenticated"
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Pass through to the next filter
        chain.doFilter(request, response);
    }
}