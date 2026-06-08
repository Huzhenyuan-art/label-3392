package com.example.lab3392.config;

import com.example.lab3392.service.AuthUserDetailsService;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthUserDetailsService userDetailsService) throws Exception {
        http
                .userDetailsService(userDetailsService)
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers("/login", "/register", "/assets/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            if (request.getParameter("remember-me") == null) {
                                Cookie c = new Cookie("remember-me", "");
                                c.setMaxAge(0);
                                c.setPath("/");
                                c.setHttpOnly(true);
                                response.addCookie(c);
                            }

                            SimpleUrlAuthenticationSuccessHandler handler = new SimpleUrlAuthenticationSuccessHandler("/products");
                            handler.setAlwaysUseDefaultTargetUrl(true);
                            handler.onAuthenticationSuccess(request, response, authentication);
                        })
                        .failureHandler(new AuthenticationFailureHandler() {
                            @Override
                            public void onAuthenticationFailure(jakarta.servlet.http.HttpServletRequest request,
                                    jakarta.servlet.http.HttpServletResponse response, AuthenticationException exception)
                                    throws IOException, jakarta.servlet.ServletException {
                                String baseUrl = "/login";
                                String redirectUrl;
                                if (exception instanceof DisabledException) {
                                    redirectUrl = UriComponentsBuilder.fromPath(baseUrl)
                                            .queryParam("disabled")
                                            .toUriString();
                                } else {
                                    redirectUrl = UriComponentsBuilder.fromPath(baseUrl)
                                            .queryParam("error")
                                            .toUriString();
                                }
                                response.sendRedirect(request.getContextPath() + redirectUrl);
                            }
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID", "remember-me")
                )
                .rememberMe(remember -> remember
                        .userDetailsService(userDetailsService)
                        .rememberMeParameter("remember-me")
                        .rememberMeCookieName("remember-me")
                        .tokenValiditySeconds(86400)
                        .key("lab3392-remember-me-key")
                )
                .exceptionHandling(ex -> ex.accessDeniedPage("/error/403"))
                .csrf(Customizer.withDefaults());

        return http.build();
    }
}
