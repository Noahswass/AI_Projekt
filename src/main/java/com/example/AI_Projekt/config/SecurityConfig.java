package com.example.AI_Projekt.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            // H2 console has its own login (JDBC URL / user / password)
            .requestMatchers(PathRequest.toH2Console()).permitAll()
            .anyRequest().authenticated())
        // H2 console posts forms without a CSRF token
        .csrf(csrf -> csrf.ignoringRequestMatchers(PathRequest.toH2Console()))
        // H2 console is rendered in frames
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
        .formLogin(Customizer.withDefaults())
        .httpBasic(Customizer.withDefaults());
    return http.build();
  }
}
