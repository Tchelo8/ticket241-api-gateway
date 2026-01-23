package com.tchelo.api_gateway_ticket241.filter;

import com.tchelo.api_gateway_ticket241.service.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;


// Filtre personnalisé pour valider les tokens JWT, appliqué aux routes protégées (tous mes micro services)
@Component
@Slf4j
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Autowired
    private JwtService jwtService;
    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            log.info("Filtre JWT - Requête: {} {}", request.getMethod(), request.getURI());

            // 1. Vérifier si le header Authorization existe
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.warn("Aucun header Authorization");
                return onError(exchange, "Authorization header manquant", HttpStatus.UNAUTHORIZED);
            }

            // 2. Extraire le token
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Format Authorization invalide");
                return onError(exchange, "Format Authorization invalide. Utilisez: Bearer <token>", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            log.debug("Token extrait: {}...", token.substring(0, Math.min(20, token.length())));

            // 3. Valider le token
            if (!jwtService.isTokenValid(token)) {
                log.warn("Token invalide ou expiré");
                return onError(exchange, "Token invalide ou expiré", HttpStatus.UNAUTHORIZED);
            }

            // 4. Extraire les informations du token
            try {
                String phoneNumber = jwtService.extractPhoneNumber(token);
                List<String> roles = jwtService.extractRoles(token);

                log.info("Token valide - Utilisateur: {} - Rôles: {}", phoneNumber, roles);

                // 5. Ajouter les headers pour les services downstream
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", phoneNumber)
                        .header("X-User-Roles", String.join(",", roles))
                        .build();

                log.debug("Headers ajoutés: X-User-Id={}, X-User-Roles={}", phoneNumber, roles);

                // 6. Continuer avec la requête modifiée
                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                log.error("Erreur extraction token: {}", e.getMessage());
                return onError(exchange, "Erreur lors du traitement du token", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    //Gestion des erreurs : renvoie une réponse JSON
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");

        String errorResponse = String.format(
                "{\"status\": %d, \"message\": \"%s\", \"timestamp\": \"%s\"}",
                status.value(),
                message,
                java.time.LocalDateTime.now()
        );

        byte[] bytes = errorResponse.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        org.springframework.core.io.buffer.DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }

    // Classe de configuration (vide mais obligatoire)
    public static class Config {
        // Configuration future si nécessaire ici.
    }
}
