package com.example.client;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @Bean
    RouteLocator gateway(RouteLocatorBuilder rlb) {
        return rlb
                .routes()
                .route(rs -> rs
                        .path("/proxy")
                        .filters(
                                fs -> fs
                                        .setPath("/customers")
                                        .retry(10)
                                        .addResponseHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                        )
                        .uri("http://localhost:8080/")

                )
                .build();
    }

    @Bean
    ApplicationRunner applicationRunner(CustomerHttpClient http) {
        return a -> http.customers().subscribe(System.out::println);
    }

    @Bean
    CustomerHttpClient httpClient(WebClient.Builder builder) {
        var wc = builder.baseUrl("http://localhost:8080/").build();
        return HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(wc))
                .build()
                .createClient(CustomerHttpClient.class);
    }
}

record Profile(Integer id) {
}


@Controller
class CustomerGraphqlController {

    private final CustomerHttpClient http;

    CustomerGraphqlController(CustomerHttpClient http) {
        this.http = http;
    }

    @QueryMapping
    Flux<Customer> customersByName(@Argument String name) {
        return this.http.customersByName(name);
    }

    @QueryMapping
    Flux<Customer> customers() {
        return this.http.customers();
    }

    @BatchMapping
    Map<Customer, Profile> profile(List<Customer> customers) throws Exception {
        var m = new HashMap<Customer, Profile>();
        for (var c : customers)
            m.put(c, new Profile(c.id()));
        System.out.println("got a profile [" + m.toString() + "]");
        return m;
    }
}


interface CustomerHttpClient {

    @GetExchange("/customers")
    Flux<Customer> customers();

    @GetExchange("/customers/{name}")
    Flux<Customer> customersByName(@PathVariable String name);
}


record Customer(Integer id, String name) {
}


