package com.clickmunch.APIGateway.config;

import org.springframework.beans.factory.annotation.Value;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.rewritePath;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import static org.springframework.web.servlet.function.RequestPredicates.path;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import com.clickmunch.APIGateway.security.JwtAuthenticationFilter;
import com.clickmunch.APIGateway.security.JwtTokenUtil;


/**
 * API Gateway routing.
 *
 * All REST traffic is unified under this gateway (port 8080) with JWT
 * enforcement for protected services.
 *
 * WebSocket traffic (OrderService kitchen events) is NOT routed here.
 * Spring Cloud Gateway Server MVC (servlet) does not transparently proxy
 * the HTTP Upgrade handshake; only the reactive flavor does. Migrating this
 * gateway to reactive is out of scope for the order-service feature, so the
 * realtime channel follows the common "REST gateway + separate realtime
 * channel" pattern used in production (AWS API Gateway + AppSync, nginx with
 * split paths, etc.). Clients connect directly to:
 *     ws://localhost:8085/ws/kitchen
 * If a single edge is required later, add an nginx/traefik sidecar in front
 * that terminates both HTTP and WebSocket on one port.
 */
@Configuration
public class RouteConfig {

    @Value("${services.auth.url:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${services.restaurant.url:http://localhost:8082}")
    private String restaurantServiceUrl;

    @Value("${services.menu.url:http://localhost:8084}")
    private String menuServiceUrl;

    @Value("${services.order.url:http://localhost:8085}")
    private String orderServiceUrl;

    @Value("${services.reservation.url:http://localhost:8086}")
    private String reservationServiceUrl;

    @Value("${services.checkout.url:http://localhost:8089}")
    private String checkoutServiceUrl;

    @Value("${services.rating.url:http://localhost:8088}")
    private String ratingServiceUrl;

    @Value("${services.notification.url:http://localhost:8087}")
    private String notificationServiceUrl;

    @Bean
    public RouterFunction<ServerResponse> routes(JwtTokenUtil jwtTokenUtil) {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenUtil);

        // Rewrite pattern "^/<prefix>(/.*)?$" covers both the bare prefix
        // (POST /order  -> /api/orders) and any subpath
        // (GET /order/restaurant/1 -> /api/orders/restaurant/1). The previous
        // pattern required a mandatory "/" after the prefix, which broke root
        // POSTs through the gateway.
        RouterFunction<ServerResponse> auth = route("auth")
                .route(path("/auth/**"), http())
                .before(uri(authServiceUrl))
                .before(rewritePath("^/auth(/.*)?$", "/api/auth$1"))
                .build();
        RouterFunction<ServerResponse> restaurant = route("restaurant")
                .route(path("/restaurant/**"), http())
                .before(uri(restaurantServiceUrl))
                .before(rewritePath("^/restaurant(/.*)?$", "/api/restaurants$1"))
                .filter(jwtAuthenticationFilter)
                .build();
        RouterFunction<ServerResponse> menu = route("menu")
                .route(path("/menu/**"), http())
                .before(uri(menuServiceUrl))
                .before(rewritePath("^/menu(/.*)?$", "/api/menus$1"))
                .filter(jwtAuthenticationFilter)
                .build();
        RouterFunction<ServerResponse> order = route("order")
                .route(path("/order/**"), http())
                .before(uri(orderServiceUrl))
                .before(rewritePath("^/order(/.*)?$", "/api/orders$1"))
                .filter(jwtAuthenticationFilter)
                .build();

        RouterFunction<ServerResponse> reservation = route("reservation")
            .route(path("/reservation/**"), http())
            .before(uri(reservationServiceUrl))
            .before(rewritePath("^/reservation(/.*)?$", "/api/reservations$1"))
            .filter(jwtAuthenticationFilter)
            .build();

        RouterFunction<ServerResponse> checkout = route("checkout")
            .route(path("/checkout/**"), http())
            .before(uri(checkoutServiceUrl))
            .before(rewritePath("^/checkout(/.*)?$", "/api/checkout$1"))
            .filter(jwtAuthenticationFilter)
            .build();

        RouterFunction<ServerResponse> rating = route("rating")
            .route(path("/rating/**"), http())
            .before(uri(ratingServiceUrl))
            .before(rewritePath("^/rating(/.*)?$", "/api/ratings$1"))
            .filter(jwtAuthenticationFilter)
            .build();

        RouterFunction<ServerResponse> notification = route("notification")
            .route(path("/notification/**"), http())
            .before(uri(notificationServiceUrl))
            .before(rewritePath("^/notification(/.*)?$", "/api/notifications$1"))
            .filter(jwtAuthenticationFilter)
            .build();

        return auth
            .and(restaurant)
            .and(menu)
            .and(order)
            .and(reservation)
            .and(checkout)
            .and(rating)
            .and(notification);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }


}
