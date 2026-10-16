package com.tchelo.api_gateway_ticket241;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

// Lit les routes du vrai application.yaml : l'achat sans compte est public, mais seulement lui,
// et un client ne peut pas y injecter une identité
class GuestTicketRouteTest {

    private static Properties routes;
    private static String guestRoute;

    @BeforeAll
    static void loadRoutes() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yaml"));
        routes = yaml.getObject();
        for (int i = 0; routes.containsKey("spring.cloud.gateway.routes[" + i + "].id"); i++) {
            if ("ticket-service-guest-route".equals(routes.getProperty("spring.cloud.gateway.routes[" + i + "].id"))) {
                guestRoute = "spring.cloud.gateway.routes[" + i + "]";
            }
        }
        assertThat(guestRoute).as("route ticket-service-guest-route").isNotNull();
    }

    private static boolean isGuestPath(String path) {
        String predicate = routes.getProperty(guestRoute + ".predicates[0]");
        return new PathPatternParser().parse(predicate.substring("Path=".length()))
                .matches(PathContainer.parsePath(path));
    }

    @Test
    void guestPurchaseFlowIsPublic() {
        assertThat(isGuestPath("/api/tickets/guest/reserve")).isTrue();
        assertThat(isGuestPath("/api/tickets/guest/orders/3f2b1c4d-1111-2222-3333-444455556666/purchase")).isTrue();
        assertThat(isGuestPath("/api/tickets/guest/access/verify")).isTrue();
    }

    @Test
    void accountRoutesStayBehindTheJwt() {
        assertThat(isGuestPath("/api/tickets/reserve")).isFalse();
        assertThat(isGuestPath("/api/tickets/my-tickets")).isFalse();
        assertThat(isGuestPath("/api/tickets/order/3f2b1c4d-1111-2222-3333-444455556666/claim-guest")).isFalse();
        assertThat(isGuestPath("/api/tickets/validate")).isFalse();
    }

    @Test
    void guestRouteStripsIdentityAndComesFirst() {
        assertThat(routes.getProperty(guestRoute + ".order")).isEqualTo("-1");
        List<String> filters = new ArrayList<>();
        for (int f = 0; routes.containsKey(guestRoute + ".filters[" + f + "]"); f++) {
            filters.add(routes.getProperty(guestRoute + ".filters[" + f + "]"));
        }
        assertThat(filters).contains(
                "RemoveRequestHeader=X-User-Id", "RemoveRequestHeader=X-User-Roles",
                "RemoveRequestHeader=X-User-Email", "RemoveRequestHeader=X-User-Structure-Id",
                "RemoveRequestHeader=X-User-Account-Id");
        assertThat(filters).doesNotContain("JwtAuthenticationFilter");
    }
}
