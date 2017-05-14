package daggerok;

import com.fasterxml.jackson.databind.ObjectMapper;
import daggerok.data.Activity;
import daggerok.data.ActivityRepository;
import daggerok.data.Task;
import daggerok.data.TaskRepository;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
@EnableRedisRepositories(basePackageClasses = ActivityRepository.class)
public class ReactiveWebfluxSpringDataRedisApplication {

  @Autowired ObjectMapper objectMapper;
/*
  @Bean
  public RedisConnection redisConnection(final RedisConnectionFactory redisConnectionFactory) {
    return redisConnectionFactory.getConnection();
  }
*/
  @Bean
  public ReactiveRedisConnection reactiveRedisConnection(final ReactiveRedisConnectionFactory redisConnectionFactory) {
    return redisConnectionFactory.getReactiveConnection();
  }

  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    return new LettuceConnectionFactory();
  }

  @Bean
  public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
    return new LettuceConnectionFactory();
  }

  private String json(Object o) {

    return Try.of(() -> objectMapper.writerWithDefaultPrettyPrinter()
                                    .writeValueAsString(o))
              .get();
  }

  @Bean
  @Transactional
  public CommandLineRunner testData(final TaskRepository taskRepository,
                                    final ActivityRepository activityRepository) {

    if (taskRepository.count() > 0 || activityRepository.count() > 0)
      return args -> log.info("test-data already exists...\n{}\n{}",
                              json(taskRepository.findAll()),
                              json(activityRepository.findAll()));

    return args -> Flux.merge(Mono.just(taskRepository.count()),
                              Mono.just(activityRepository.count()))
                       .filter(aLong -> aLong == 0)
                       .subscribe(aLong -> activityRepository.saveAll(
                           Flux.just("one", "two", "three")
                               .map(i -> new Task().setBody(i))
                               .map(taskRepository::save)
                               .map(t -> new Activity().setTasks(singletonList(t)))
                               .toIterable()));

  }

  @Bean
  public RouterFunction<ServerResponse> routes(final TaskRepository taskRepository,
                                               final ActivityRepository activityRepository,
                                               final ReactiveRedisConnection connection) {

    val keyCommands = connection.keyCommands();

    return

        route(

            DELETE("/"),
            request -> ok().body(
                Mono.fromCallable(() -> {
                  activityRepository.deleteAll();
                  taskRepository.deleteAll();
                  return "done.";
                }).subscribe(), String.class))

        .andRoute(

            GET("/tasks"),
            request -> ok().body(Flux.fromIterable(taskRepository.findAll()), Task.class))

        .andRoute(

            GET("/activities"),
            request -> ok().body(Flux.fromIterable(activityRepository.findAll()), Activity.class))

        .andRoute(

            GET("/**"),
            request -> ok().body(Mono.just(format("command type %s",
                                                  keyCommands.randomKey()
                                                             .flatMap(keyCommands::type)
                                                             .map(DataType::code)
                                                             .subscribe())), String.class))
        ;
  }

  public static void main(String[] args) {
    SpringApplication.run(ReactiveWebfluxSpringDataRedisApplication.class, args);
  }
}
