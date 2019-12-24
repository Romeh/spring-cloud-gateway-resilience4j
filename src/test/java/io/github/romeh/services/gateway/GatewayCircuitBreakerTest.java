package io.github.romeh.services.gateway;


import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import org.junit.*;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.MockServerContainer;
import io.github.romeh.services.gateway.controller.FallbackResponse;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.mockserver.model.HttpResponse.response;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@RunWith(SpringRunner.class)
public class GatewayCircuitBreakerTest {


    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayCircuitBreakerTest.class);

    @Rule
    public TestRule benchmarkRun = new BenchmarkRule();

    @ClassRule
    public static MockServerContainer mockServer = new MockServerContainer();

    @Autowired
    TestRestTemplate template;
    final Random random = new Random();
    int i = 0;

    @BeforeClass
    public static void init() {
        // override the routes with the circuit breaker filter
        System.setProperty("spring.cloud.gateway.routes[0].id", "test-service-withResilient4j");
        System.setProperty("spring.cloud.gateway.routes[0].uri", "http://192.168.99.100:" + mockServer.getServerPort());
        System.setProperty("spring.cloud.gateway.routes[0].predicates[0]", "Path=/testService/**");
        System.setProperty("spring.cloud.gateway.routes[0].filters[0]", "RewritePath=/testService/(?<path>.*), /$\\{path}");
        System.setProperty("spring.cloud.gateway.routes[0].filters[1].name", "CircuitBreaker");
        System.setProperty("spring.cloud.gateway.routes[0].filters[1].args.name", "backendA");
        System.setProperty("spring.cloud.gateway.routes[0].filters[1].args.fallbackUri", "forward:/fallback/testService");
        MockServerClient client = new MockServerClient(mockServer.getContainerIpAddress(), mockServer.getServerPort());
        client.when(HttpRequest.request()
                .withPath("/1"))
                .respond(response()
                        .withBody("{\"msgCode\":\"1\",\"msg\":\"1234567890\"}")
                        .withHeader("Content-Type", "application/json"));
        client.when(HttpRequest.request()
                .withPath("/2"), Times.exactly(5))
				.respond(response()
                        .withBody("{\"msgCode\":\"2\",\"msg\":\"1234567891\"}")
                        .withDelay(TimeUnit.MILLISECONDS, 200)
                        .withHeader("Content-Type", "application/json"));
        client.when(HttpRequest.request()
                .withPath("/2"))
                .respond(response()
                        .withBody("{\"msgCode\":\"2\",\"msg\":\"1234567891\"}")
                        .withHeader("Content-Type", "application/json"));
    }

    @Test
    @BenchmarkOptions(warmupRounds = 0, concurrency = 1, benchmarkRounds = 200)
    public void testAccountService() {
        int gen = 1 + (i++ % 2);
        ResponseEntity<FallbackResponse> r = template.exchange("/testService/{id}", HttpMethod.GET, null, FallbackResponse.class, gen);
        LOGGER.info("{}. Received: status->{}, payload->{}, call->{}", i, r.getStatusCodeValue(), r.getBody(), gen);
    }

}
