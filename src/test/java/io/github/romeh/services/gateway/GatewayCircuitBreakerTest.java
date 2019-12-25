package io.github.romeh.services.gateway;


import static org.mockserver.model.HttpResponse.response;

import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.MockServerContainer;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;

import io.github.romeh.services.gateway.controller.FallbackResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = {GatewayCircuitBreakerTest.Initializer.class})
public class GatewayCircuitBreakerTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(GatewayCircuitBreakerTest.class);
	private static MockServerContainer mockServerContainer;

	static {
		mockServerContainer = new MockServerContainer();
		mockServerContainer.start();

	}

	static class Initializer
			implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
			TestPropertyValues.of(
					"spring.cloud.gateway.routes[0].id=test-service-withResilient4j",
					"spring.cloud.gateway.routes[0].uri=" + mockServerContainer.getEndpoint(),
					"spring.cloud.gateway.routes[0].predicates[0]=" + "Path=/testService/**",
					"spring.cloud.gateway.routes[0].filters[0]=" + "RewritePath=/testService/(?<path>.*), /$\\{path}",
					"spring.cloud.gateway.routes[0].filters[1].name=" + "CircuitBreaker",
					"spring.cloud.gateway.routes[0].filters[1].args.name=" + "backendA",
					"spring.cloud.gateway.routes[0].filters[1].args.fallbackUri=" + "forward:/fallback/testService"
			).applyTo(configurableApplicationContext.getEnvironment());
		}
	}

	@Rule
	public TestRule benchmarkRun = new BenchmarkRule();
	private MockServerClient client = new MockServerClient(mockServerContainer.getContainerIpAddress(), mockServerContainer.getServerPort());
	@Autowired
	private TestRestTemplate template;
	private int testPathSelector = 0;

	@Before
	public void init() {
		client.when(HttpRequest.request()
				.withPath("/1"))
				.respond(response()
						.withBody("{\"msgCode\":\"1\",\"msg\":\"1000000\"}")
						.withHeader("Content-Type", "application/json"));
		client.when(HttpRequest.request()
				.withPath("/2"), Times.exactly(5))
				.respond(response()
						.withBody("{\"msgCode\":\"2\",\"msg\":\"2000000\"}")
						.withDelay(TimeUnit.MILLISECONDS, 200)
						.withHeader("Content-Type", "application/json"));
		client.when(HttpRequest.request()
				.withPath("/2"))
				.respond(response()
						.withBody("{\"msgCode\":\"2\",\"msg\":\"2100000\"}")
						.withHeader("Content-Type", "application/json"));
	}

	@Test
	@BenchmarkOptions(warmupRounds = 0, concurrency = 1, benchmarkRounds = 200)
	public void testAccountService() {
		int generatedPathValue = 1 + (testPathSelector++ % 2);
		ResponseEntity<FallbackResponse> r = template.exchange("/testService/{id}", HttpMethod.GET, null, FallbackResponse.class, generatedPathValue);
		LOGGER.info("{}. Received: status->{}, payload->{}, call->{}", generatedPathValue, r.getStatusCodeValue(), r.getBody(), generatedPathValue);
	}

	@AfterClass
    public static void close(){
	    mockServerContainer.close();
    }
}
