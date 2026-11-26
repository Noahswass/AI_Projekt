package com.example.AI_Projekt.config;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

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
            // The error page must be shown to everyone, or a visitor's 404 turns into a login prompt
            .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
            .requestMatchers("/error").permitAll()
            // Stylesheets and scripts (/css/**, /js/**, /images/**, ...), or visitors get unstyled pages
            .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
            // The public catalog is read-only and open to visitors
            .requestMatchers(HttpMethod.GET, "/catalog/**").permitAll()
            // The landing page is where visitors start, so it is open too
            .requestMatchers(HttpMethod.GET, "/").permitAll()
            // Anyone can register (GET shows the form, POST creates the account)
            .requestMatchers("/register").permitAll()
            //Anyone can access assistant page
            .requestMatchers("/assistant").permitAll()
            // formLogin's own permitAll() covers /login, /login?error and /login?logout exactly.
            // This line opens the page with any other parameter too, such as /login?registered.
            .requestMatchers(HttpMethod.GET, "/login").permitAll()
            // Admin pages, such as starting a catalog sync by hand
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated())
            // Our own login page instead of the generated one; the users come from AppUserDetailsService
            .formLogin(form -> form.loginPage("/login").permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
            .httpBasic(Customizer.withDefaults());
    return http.build();
  }
}
