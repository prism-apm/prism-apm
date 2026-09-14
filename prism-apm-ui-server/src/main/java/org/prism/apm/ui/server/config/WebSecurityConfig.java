package org.prism.apm.ui.server.config;

import org.prism.apm.ui.server.security.CustomAuthorizationManager;
import org.prism.apm.ui.server.security.CustomUserDetailsService;
import org.prism.apm.ui.server.security.JwtAuthenticationFilter; // [Import]
import org.prism.apm.ui.server.security.JwtHelper; // [Import]
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // [Import]
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final CustomAuthorizationManager customAuthorizationManager;
    private final JwtAuthenticationFilter jwtAuthenticationFilter; // [Added injection]
    private JwtHelper jwtHelper; // [Added injection]

    public WebSecurityConfig(CustomUserDetailsService customUserDetailsService,
                             CustomAuthorizationManager customAuthorizationManager,
                             JwtAuthenticationFilter jwtAuthenticationFilter,
                             JwtHelper jwtHelper) {
        this.customUserDetailsService = customUserDetailsService;
        this.customAuthorizationManager = customAuthorizationManager;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtHelper = jwtHelper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                // 1. Core: stateless Session, do not create HttpSession
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**").permitAll()
                        // Ensure the login endpoint allows anonymous access
                        .requestMatchers("/api/apm/login").permitAll()
                        .anyRequest().access(customAuthorizationManager)
                )

                // 2. Core: add the JWT Filter to the filter chain before the username-password filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .formLogin(form -> form
                        .loginProcessingUrl("/api/apm/login")
                        .successHandler(successHandler) // Use the modified Handler
                        .failureHandler(failureHandler)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/api/apm/logout")
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .permitAll()
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(exceptionEntry)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }

    // --- Handlers ---

    // [Changed] Login success: generate Token and return it
    private final AuthenticationSuccessHandler successHandler = (request, response, authentication) -> {
        // 1. Get the username from the successful login
        String username = authentication.getName();
        // 2. Use JwtUtil to generate Token
        String token = jwtHelper.generateToken(username);

        // 3. Return JSON
        response.setContentType("application/json;charset=UTF-8");
        // Response format example：{"success": true, "message": "...", "token": "eyJh..."}
        response.getWriter().write("{\"success\": true, \"message\": \"Login successful\", \"token\": \"" + token + "\"}");
    };

    private final AuthenticationFailureHandler failureHandler = (request, response, exception) -> {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"success\": false, \"message\": \"" + exception.getMessage() + "\"}");
    };

    // [Note] After the frontend receives this response, it should delete the Token on the client
    private final LogoutSuccessHandler logoutSuccessHandler = (request, response, authentication) -> {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\": true, \"message\": \"Logout successful\"}");
    };

    private final AuthenticationEntryPoint exceptionEntry = (request, response, authException) -> {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"success\": false, \"message\": \"" + authException.getMessage() + "\"}");
    };

    private final AccessDeniedHandler accessDeniedHandler = (request, response, accessDeniedException) -> {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
        // Keep the JSON format consistent with other endpoints
        response.getWriter().write("{\"success\": false, \"message\": \"Insufficient permissions: " + accessDeniedException.getMessage() + "\"}");
    };
}