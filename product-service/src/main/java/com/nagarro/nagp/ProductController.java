package com.nagarro.nagp;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreaker;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class ProductController {

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private CircuitBreakerFactory circuitBreakerFactory;

    @GetMapping("/instances")
    public List<ServiceInstance> getInstances() {
        return discoveryClient.getInstances("CURRENCY-SERVICE");
    }

    @GetMapping("/product-info/{id}")
    public String getProductInfo(@PathVariable("id") int productId, @RequestParam("currency") String currency) {
        Map<Integer, Double> productPrices = new HashMap<>();
        productPrices.put(1, 100.0);
        productPrices.put(2, 150.0);
        productPrices.put(3, 250.0);

        RestTemplate restClient = new RestTemplate();
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("circuitbreaker");
        
        Double convRate = circuitBreaker.run(() -> {
            System.out.println("Attempt");
            List<ServiceInstance> currencyService = discoveryClient.getInstances("CURRENCY-SERVICE");
            URI currencyUri = currencyService.get(0).getUri();
            return restClient.getForObject(currencyUri + "/conversion-rate?from=USD&to=" + currency, Double.class);
        }, throwable -> {
            return 10000000.0;
        });
        return "Product " + productId + " has price =" + convRate * productPrices.get(productId) + " " + currency;
    }
}
