package com.example.AI_Projekt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled) throws Exception {

    // PathRequest.toH2Console() only works when the console is switched on (dev profile).
    // Without this check every request fails when the console is off.
    if (h2ConsoleEnabled) {
      http
          // H2 console has its own login (JDBC URL / user / password)
          .authorizeHttpRequests(auth -> auth.requestMatchers(PathRequest.toH2Console()).permitAll())
          // H2 console posts forms without a CSRF token
          .csrf(csrf -> csrf.ignoringRequestMatchers(PathRequest.toH2Console()))
          // H2 console is rendered in frames
          .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
    }

    http
        .authorizeHttpRequests(auth -> auth
            // The public catalog is read-only and open to visitors
            .requestMatchers(HttpMethod.GET, "/catalog/**").permitAll()
            // The landing page is where visitors start, so it is open too
            .requestMatchers(HttpMethod.GET, "/").permitAll()
            .anyRequest().authenticated())
        .formLogin(Customizer.withDefaults())
        .httpBasic(Customizer.withDefaults());
    return http.build();
  }
}
