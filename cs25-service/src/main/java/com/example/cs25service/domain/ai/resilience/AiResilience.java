package com.example.cs25service.domain.ai.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class AiResilience {

    private final CircuitBreakerRegistry cbRegistry;
    private final RetryRegistry retryRegistry;

    /**
     * 동기 호출: Retry → CircuitBreaker 순서
     */
    public <T> T executeSync(String name, Supplier<T> supplier) {
        CircuitBreaker cb = cbRegistry.circuitBreaker(name);
        Retry retry = retryRegistry.retry(name);

        Supplier<T> withRetry = Retry.decorateSupplier(retry, supplier);
        Supplier<T> withCb = CircuitBreaker.decorateSupplier(cb, withRetry);

        return withCb.get();
    }

    /**
     * Flux 스트리밍: RetryOperator → CircuitBreakerOperator 순서
     */
    public <T> Flux<T> executeStream(String name, Supplier<Flux<T>> supplier) {
        CircuitBreaker cb = cbRegistry.circuitBreaker(name);
        Retry retry = retryRegistry.retry(name);

        return supplier.get()
            .transformDeferred(RetryOperator.of(retry))
            .transformDeferred(CircuitBreakerOperator.of(cb));
    }
}
