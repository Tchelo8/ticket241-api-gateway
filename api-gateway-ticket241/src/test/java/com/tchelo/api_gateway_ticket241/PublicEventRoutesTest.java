package com.tchelo.api_gateway_ticket241;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

// Lit les routes du vrai application.yaml : vérifie qu'un visiteur atteint bien les pages publiques des
// événements, et qu'aucune route de gestion ne passe par la route publique (sans JWT)
class PublicEventRoutesTest {

    private static List<PathPattern> publicReadPatterns;
    private static List<PathPattern> publicViewPatterns;
    private static Properties routes;

    @BeforeAll
    static void loadRoutes() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yaml"));
        routes = yaml.getObject();
        publicReadPatterns = patternsOf("event-service-public-read-route");
        publicViewPatterns = patternsOf("event-service-public-view-route");
    }

    private static List<PathPattern> patternsOf(String routeId) {
        for (int i = 0; routes.containsKey("spring.cloud.gateway.routes[" + i + "].id"); i++) {
            if (routeId.equals(routes.getProperty("spring.cloud.gateway.routes[" + i + "].id"))) {
                for (int p = 0; routes.containsKey("spring.cloud.gateway.routes[" + i + "].predicates[" + p + "]"); p++) {
                    String predicate = routes.getProperty("spring.cloud.gateway.routes[" + i + "].predicates[" + p + "]");
                    if (predicate.startsWith("Path=")) {
                        PathPatternParser parser = new PathPatternParser();
                        return Arrays.stream(predicate.substring("Path=".length()).split(","))
                                .map(parser::parse)
                                .toList();
                    }
                }
            }
        }
        throw new IllegalStateException("Route introuvable : " + routeId);
    }

    private static boolean isPublicRead(String path) {
        return publicReadPatterns.stream().anyMatch(p -> p.matches(PathContainer.parsePath(path)));
    }

    @Test
    void visitorsCanBrowseEvents() {
        assertThat(isPublicRead("/api/events/all")).isTrue();
        assertThat(isPublicRead("/api/events/search")).isTrue();
        assertThat(isPublicRead("/api/events/upcoming")).isTrue();
        assertThat(isPublicRead("/api/events/featured")).isTrue();
        assertThat(isPublicRead("/api/events/slug/concert-2026")).isTrue();
        assertThat(isPublicRead("/api/events/category/CONCERT")).isTrue();
        assertThat(isPublicRead("/api/events/city/3")).isTrue();
        assertThat(isPublicRead("/api/events/3f2b1c4d-1111-2222-3333-444455556666")).isTrue();
        assertThat(isPublicRead("/api/events/12/categories/4/availability")).isTrue();
        assertThat(isPublicRead("/api/events/categories/get/all")).isTrue();
        assertThat(isPublicRead("/api/events/cities/get/all/active")).isTrue();
    }

    @Test
    void managementRoutesStayBehindTheJwt() {
        assertThat(isPublicRead("/api/events/my-events")).isFalse();
        assertThat(isPublicRead("/api/events/admin/slug/concert-2026")).isFalse();
        assertThat(isPublicRead("/api/events/structure/1")).isFalse();
        assertThat(isPublicRead("/api/events/structures/all")).isFalse();
        assertThat(isPublicRead("/api/events/id/1")).isFalse();
        assertThat(isPublicRead("/api/events/3f2b1c4d-1111-2222-3333-444455556666/publish")).isFalse();
    }

    @Test
    void onlyTheViewCounterIsPublicInPost() {
        assertThat(publicViewPatterns).hasSize(1);
        assertThat(publicViewPatterns.get(0).matches(
                PathContainer.parsePath("/api/events/3f2b1c4d-1111-2222-3333-444455556666/view"))).isTrue();
        assertThat(publicViewPatterns.get(0).matches(
                PathContainer.parsePath("/api/events/3f2b1c4d-1111-2222-3333-444455556666/publish"))).isFalse();
    }

    @Test
    void publicRoutesAreReadOnlyStripIdentityAndComeFirst() {
        String read = routeIndex("event-service-public-read-route");
        assertThat(routes.getProperty(read + ".predicates[0]")).isEqualTo("Method=GET");
        assertThat(routes.getProperty(read + ".order")).isEqualTo("-1");
        List<String> filters = List.of(
                routes.getProperty(read + ".filters[0]"), routes.getProperty(read + ".filters[1]"),
                routes.getProperty(read + ".filters[2]"), routes.getProperty(read + ".filters[3]"),
                routes.getProperty(read + ".filters[4]"));
        assertThat(filters).containsExactlyInAnyOrder(
                "RemoveRequestHeader=X-User-Id", "RemoveRequestHeader=X-User-Roles",
                "RemoveRequestHeader=X-User-Email", "RemoveRequestHeader=X-User-Structure-Id",
                "RemoveRequestHeader=X-User-Account-Id");
    }

    private static String routeIndex(String routeId) {
        for (int i = 0; routes.containsKey("spring.cloud.gateway.routes[" + i + "].id"); i++) {
            if (routeId.equals(routes.getProperty("spring.cloud.gateway.routes[" + i + "].id"))) {
                return "spring.cloud.gateway.routes[" + i + "]";
            }
        }
        throw new IllegalStateException("Route introuvable : " + routeId);
    }
}
