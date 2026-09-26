package com.chaoticteam.backend.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.chaoticteam.backend.auth.services.AuthCookieService;
import com.chaoticteam.backend.auth.services.UserDetailsServiceImp;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.Cookie;

@Configuration
public class SecurityConfig {

    /** Public for every HTTP method. */
    private static final String[] PUBLIC_ANY = {
        "/api/health",
        "/api/version",
        "/api/auth/login",
        "/api/auth/signup",
        "/api/auth/refresh",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/**"
    };

    /** Public read-only endpoints (same as the go-server routes without RequireAuth). */
    private static final String[] PUBLIC_GET = {
        "/api/commentaries",
        "/api/courses",
        "/api/achievements",
        "/api/projects",
        "/api/galleries",
        "/api/profile",
        "/api/users",
        "/api/vcard/**"
    };

    @Value("${jwt.secret}")
    private String secret;

    @Value("${app.cors.allowed-origin-patterns:*}")
    private List<String> allowedOriginPatterns;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, BearerTokenResolver bearerTokenResolver) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_ANY).permitAll()
                .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                .anyRequest().authenticated() // El resto de los endpoints requieren autenticación
            )
            .httpBasic(Customizer.withDefaults()) // Configurar autenticación básica
            .oauth2ResourceServer(oauth2 -> oauth2
            .bearerTokenResolver(bearerTokenResolver)
            .jwt(jwt -> jwt
                .decoder(jwtDecoder())
            )
            );
        return http.build();
    }

    /**
     * Reads the JWT from the Authorization header and, like go-server, falls
     * back to the `access_token` cookie. Public endpoints ignore any token so a
     * stale cookie can never turn a public request into a 401.
     */
    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver header = new DefaultBearerTokenResolver();
        RequestMatcher publicRequests = new OrRequestMatcher(
            Stream.concat(
                Arrays.stream(PUBLIC_ANY).map(AntPathRequestMatcher::new),
                Arrays.stream(PUBLIC_GET).map(pattern -> new AntPathRequestMatcher(pattern, HttpMethod.GET.name()))
            ).toArray(RequestMatcher[]::new)
        );
        return request -> {
            if (publicRequests.matches(request)) {
                return null;
            }
            String token = header.resolve(request);
            if (token != null || request.getCookies() == null) {
                return token;
            }
            return Arrays.stream(request.getCookies())
                .filter(cookie -> AuthCookieService.NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        configuration.setAllowedMethods(List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret.getBytes(), SignatureAlgorithm.HS256.getJcaName())).build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsServiceImp userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
