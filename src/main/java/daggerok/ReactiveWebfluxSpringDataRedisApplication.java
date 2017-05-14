package daggerok;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class ReactiveWebfluxSpringDataRedisApplication {

  @Bean
  public RouterFunction<ServerResponse> routes() {
    return
        route(
            GET("/**"),
            request -> ServerResponse.ok().body(Mono.just("hi"), String.class));
  }

  public static void main(String[] args) {
    SpringApplication.run(ReactiveWebfluxSpringDataRedisApplication.class, args);
  }
}
