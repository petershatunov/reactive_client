package com.example.reactive_client;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.netflix.hystrix.HystrixCommands;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
public class Config {

    @Bean
    SecurityWebFilterChain authorization(ServerHttpSecurity serverHttpSecurity) {
        serverHttpSecurity.httpBasic();
        serverHttpSecurity.csrf().disable();
        serverHttpSecurity.authorizeExchange()
                .pathMatchers("/proxy").authenticated()
                .anyExchange().permitAll();
        return serverHttpSecurity.build();
    }

    @Bean
    MapReactiveUserDetailsService auth() {
        UserDetails build = User.withDefaultPasswordEncoder().username("user").password("pass").roles("USER").build();
        return new MapReactiveUserDetailsService(build);
    }

    @Bean
    RouteLocator routeLocator(RouteLocatorBuilder rlb) {
        return rlb
                .routes()
                .route(predicateSpec -> predicateSpec.path("/proxy")
                        .filters(
                            gatewayFilterSpec -> gatewayFilterSpec.setPath("/reservations")
                        )
                        .uri("http://localhost:8080")
                )
                .build();
    }

    @Bean
    RouterFunction<ServerResponse> routes(ReservationClient client) {
        return RouterFunctions.route(RequestPredicates.GET("/reservation/names"), serverRequest -> {
            Flux<String> reservationNames = client.getAllReservations()
                    .map(Reservation::getReservationName);

            Publisher<String> names = HystrixCommands
                    .from(reservationNames)
                    .commandName("names")
                    .fallback(Mono.just("this is just a fallback"))
                    .eager()
                    .build();

            return ServerResponse.ok().body(names, String.class);
        });
    }

    @Bean
    WebClient webClient() {
        return WebClient.builder().build();
    }
}
