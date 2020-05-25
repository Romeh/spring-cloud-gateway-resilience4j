# Spring Cloud Gateway with Resilient4J circuit breaker

In this project I'm showing how you can use spring cloud circuit-breaker starter for resilience4j with resilience4j spring boot starter so you can configure externally your circuitBreaker definitions
in:  [Spring Cloud Gateway](https://cloud.spring.io/spring-cloud-gateway/reference/html/)

## Getting Started 

Currently you may find here examples of: 
1. Circuit Breaker and Fallback with [resilience4j](https://resilience4j.readme.io/docs/getting-started) - the detailed explanation is my on blog: https://mromeh.com/2019/12/26/spring-cloud-gateway-with-resilience4j-circuit-breaker/
2. How to mock http service using mock server test containers for micro-services integration tests
3. How to configure externally in spring config Resilience4j circuit breakers and integrate it with Spring cloud circuit breaker starter
4. How to configure externally in spring config Resilience4j time limter and integrate it with Spring cloud circuit breaker starter
5. How to check http status code and make circuit breaker act accordingly in the spring cloud gateway

### Usage 
1. To build and run the main application you need to have Maven, JDK8+ and Docker.
2. To build it run command `mvn clean install`
3. During Maven build the JUnit integration tests are running. We are using [Testcontainers](https://www.testcontainers.org/) for mocking downstream service.

Feel free to propose your code changes / extensions ! 
