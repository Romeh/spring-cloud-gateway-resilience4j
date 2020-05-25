package io.github.romeh.services.gateway;

import java.util.function.Predicate;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author romeh
 */
public class HttpInternalServicePredicate implements Predicate<ResponseStatusException> {
	@Override
	public boolean test(ResponseStatusException e) {
		return e.getStatus().is5xxServerError();
	}
}
