package com.example.demo_multiple_services.service;

import com.example.demo_multiple_services.dto.ApiResponse;
import com.example.demo_multiple_services.dto.OrderDto;
import com.example.demo_multiple_services.dto.ProductDto;
import com.example.demo_multiple_services.dto.ResponseStatusDto;
import com.example.demo_multiple_services.exception.CustomException;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * One-Phase Commit BFF Service for Order
 *
 * This service coordinates distributed transactions across multiple 1PC microservices by:
 * - Starting a ScalarDB distributed transaction locally
 * - Propagating the transaction ID to 1PC services via ScalarDB-Transaction-ID HTTP header
 * - Calling 1PC REST endpoints for CRUD operations
 * - Committing or rolling back based on all services' responses
 *
 * Key concepts:
 * - Transaction ID propagation: 1PC services join the same transaction using the ID from headers
 * - Atomic operations: All services succeed together or all fail together
 * - Rollback coordination: If any service fails, all changes are rolled back
 * - ApiResponse handling: 1PC services return ApiResponse<T> for consistent response structure
 */
@Slf4j
@Service
public class PlaceOrderOnePCBffService extends BaseOnePCBffService {

    @Value("${server.port:8080}")
    private String serverPort;

    private static final String BASE_URL = "http://localhost:";

    public PlaceOrderOnePCBffService(DistributedTransactionManager manager) throws InstantiationException, IllegalAccessException {
        super(manager);
    }

    // Place Order
    public ResponseStatusDto placeOrder(OrderDto orderDto) throws CustomException {
        DistributedTransaction transaction = null;
        ProductDto productDto = ProductDto.builder()
                .id(orderDto.getProductId())
                .build();
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting distributed transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            // Get Inventory Info
            String urlGet = BASE_URL + serverPort + "/product-one-pc" + "/" + productDto.getId();
            productDto = executeGet(urlGet, headers, new ParameterizedTypeReference<ApiResponse<ProductDto>>() {});

            // Check Stock
            if(productDto.getStock() < orderDto.getOrderQty()){
                throw new RuntimeException("We are out of stock.");
            }
            // Set new stock value
            productDto.setStock(productDto.getStock() - orderDto.getOrderQty());

            String urlPut = BASE_URL + serverPort + "/product-one-pc";
            executePut(urlPut, productDto, headers);

            // Insert Order
            String url = BASE_URL + serverPort + "/order-one-pc";
            executePost(url, orderDto, headers);

            transaction.commit();
            log.info("Distributed transaction committed: {}", transactionId);

            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            log.error("Transaction failed: {}", e.getMessage(), e);
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

}
