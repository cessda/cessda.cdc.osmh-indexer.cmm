package eu.cessda.pasc.oci.configurations;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
    /**
     * Configure Spring Security to restrict access to the actuator, and allow all other access
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("ACTUATOR") // Secure actuator endpoints
                .anyRequest().permitAll() // Enable access to REST APIs without authentication
                .and().formLogin() // Enable form based login
                .and().httpBasic() // Enable basic authentication
                .and().csrf().ignoringAntMatchers("/actuator**"); // https://codecentric.github.io/spring-boot-admin/2.1.5/#_csrf_on_actuator_endpoints
    }
}
