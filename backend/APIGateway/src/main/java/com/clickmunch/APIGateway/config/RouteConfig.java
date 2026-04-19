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

    @Bean
    public RouterFunction<ServerResponse> routes(JwtTokenUtil jwtTokenUtil) {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenUtil);

        RouterFunction<ServerResponse> auth = route("auth")
                .route(path("/auth/**"), http())
                .before(uri(authServiceUrl))
                .before(rewritePath("/auth/(?<segment>.*)", "/api/auth/${segment}"))
                .build();
        RouterFunction<ServerResponse> restaurant = route("restaurant")
                .route(path("/restaurant/**"), http())
                .before(uri(restaurantServiceUrl))
                .before(rewritePath("/restaurant/(?<segment>.*)", "/api/restaurants/${segment}"))
                .filter(jwtAuthenticationFilter)
                .build();
        RouterFunction<ServerResponse> menu = route("menu")
                .route(path("/menu/**"), http())
                .before(uri(menuServiceUrl))
                .before(rewritePath("/menu/(?<segment>.*)", "/api/menus/${segment}"))
                .filter(jwtAuthenticationFilter)
                .build();
        RouterFunction<ServerResponse> order = route("order")
                .route(path("/order/**"), http())
                .before(uri(orderServiceUrl))
                .before(rewritePath("/order/(?<segment>.*)", "/api/orders/${segment}"))
                .filter(jwtAuthenticationFilter)
                .build();
        return auth.and(restaurant).and(menu).and(order);
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
