package com.example.reactive_client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Component
class ReservationClient {

    private final WebClient webClient;

    ReservationClient(WebClient web) {
        this.webClient = web;
    }

    public Flux<Reservation> getAllReservations() {
        return webClient
                .get()
                .uri("http://localhost:8080/reservations")
                .retrieve()
                .bodyToFlux(Reservation.class);
    }
}
