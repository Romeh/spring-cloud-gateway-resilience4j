package io.github.romeh.services.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

/**
 * @author romeh
 */
@Component
public class HttpStatusCodeFilter extends AbstractGatewayFilterFactory<HttpStatusCodeFilter.Config> {

	@Override
	public String name() {
		return "StatusCodeCheck";
	}

	public HttpStatusCodeFilter() {
		super(Config.class);
	}

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> chain.filter(exchange).then(
				Mono.defer(() -> {
					if (!exchange.getResponse().isCommitted() &&
							HttpStatus.INTERNAL_SERVER_ERROR.equals(exchange.getResponse().getStatusCode())) {
						return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
					}
					return Mono.empty();
				}));
	}

	public static class Config {
		// Put the configuration properties
	}
}
