package com.macro.mall.search.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 搜索服务安全配置：公开检索接口，写操作需管理令牌。
 */
@Configuration
@EnableWebSecurity
public class SearchSecurityConfig {

    @Value("${mall.search.manage-token:}")
    private String manageToken;

    @Bean
    SecurityFilterChain searchFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(registry -> registry
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/favicon.ico"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/esProduct/search/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/esProduct/recommend/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/esProduct/search/relate").permitAll()
                        .requestMatchers("/esProduct/importAll", "/esProduct/create/**", "/esProduct/delete/**")
                        .authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(new ManageTokenFilter(manageToken), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(configurer -> configurer
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":401,\"message\":\"unauthorized manage token\"}");
                        })
                );
        return http.build();
    }

    static class ManageTokenFilter extends OncePerRequestFilter {
        private final String manageToken;

        ManageTokenFilter(String manageToken) {
            this.manageToken = manageToken;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            String path = request.getRequestURI();
            boolean writeEndpoint = path.startsWith("/esProduct/importAll")
                    || path.startsWith("/esProduct/create/")
                    || path.startsWith("/esProduct/delete");
            if (!writeEndpoint) {
                filterChain.doFilter(request, response);
                return;
            }
            if (!StringUtils.hasText(manageToken)) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"manage token is not configured\"}");
                return;
            }
            String provided = request.getHeader("X-Manage-Token");
            if (manageToken.equals(provided)) {
                var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "search-manager", null, java.util.List.of());
                org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
                return;
            }
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"invalid manage token\"}");
        }
    }
}
