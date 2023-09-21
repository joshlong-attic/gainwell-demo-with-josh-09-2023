package bootiful.oauthclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OauthClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(OauthClientApplication.class, args);
    }

    @Bean
    RouteLocator gateway(RouteLocatorBuilder rlb) {
        return rlb
                .routes()
                .route(
                        rs -> rs
                                .path("/api/customers")
                                .filters(fs -> fs.tokenRelay().setPath("/customers"))
                                .uri("http://localhost:8081/")
                )
                .route(
                        rs -> rs
                                .path("/api/me")
                                .filters(fs -> fs.tokenRelay().setPath("/me"))
                                .uri("http://localhost:8081/")
                )
                .route(rs -> rs.path("/**").uri("http://localhost:8020/index.html"))
                .build();
    }
}

