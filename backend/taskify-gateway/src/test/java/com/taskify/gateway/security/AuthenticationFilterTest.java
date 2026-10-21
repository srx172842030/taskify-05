package com.taskify.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationFilterTest {

    private final AuthenticationFilter filter = new AuthenticationFilter();

    @Test
    void publicPathPassesThrough() {
        AtomicBoolean called = new AtomicBoolean(false);
        GatewayFilterChain chain = e -> {
            called.set(true);
            return Mono.empty();
        };
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/health").build());
        filter.filter(exchange, chain).block();
        assertThat(called).isTrue();
    }

    @Test
    void protectedPathWithoutTokenIsRejected() {
        GatewayFilterChain chain = e -> Mono.empty();
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/examples").build());
        filter.filter(exchange, chain).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
