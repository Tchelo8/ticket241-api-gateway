package com.tchelo.api_gateway_ticket241.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


// Filtre global appliqué à toutes les requêtes,utile pour le logging et le monitoring
@Component
@Slf4j
public class GlobalLoggingFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        log.info("Gateway - {} {} - Remote: {}",
                request.getMethod(),
                request.getURI().getPath(),
                request.getRemoteAddress());

        // Continuer et logger la réponse
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            log.info("Statut - réponse: {}", response.getStatusCode());
        }));
    }

    @Override
    public int getOrder() {
        return -1;  // S'exécute en premier
    }
}
