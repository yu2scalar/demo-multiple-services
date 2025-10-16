package com.example.demo_multiple_services.service;

import com.example.demo_multiple_services.dto.ProductDto;
import com.example.demo_multiple_services.model.Order;
import com.example.demo_multiple_services.dto.OrderDto;
import com.example.demo_multiple_services.dto.ApiResponse;
import com.example.demo_multiple_services.dto.ResponseStatusDto;
import com.example.demo_multiple_services.exception.CustomException;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.exception.transaction.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
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
public class OrderOnePCBffService extends BaseOnePCBffService {

    @Value("${server.port:8080}")
    private String serverPort;

    private static final String BASE_URL = "http://localhost:";

    public OrderOnePCBffService(DistributedTransactionManager manager) throws InstantiationException, IllegalAccessException {
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

    // Create Record
    public ResponseStatusDto insertOrder(OrderDto orderDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting distributed transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

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

    // Upsert Record
    public ResponseStatusDto upsertOrder(OrderDto orderDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting distributed transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String url = BASE_URL + serverPort + "/order-one-pc/upsert";
            executePost(url, orderDto, headers);

            transaction.commit();
            log.info("Distributed transaction committed: {}", transactionId);

            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve Record
    public OrderDto getOrder(OrderDto orderDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting distributed transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String url = BASE_URL + serverPort + "/order-one-pc" + "/" + orderDto.getId();
            OrderDto result = executeGet(url, headers, new ParameterizedTypeReference<ApiResponse<OrderDto>>() {});

            transaction.commit();
            log.info("Distributed transaction committed: {}", transactionId);

            return result;
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Update Record
    public ResponseStatusDto updateOrder(OrderDto orderDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting distributed transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String url = BASE_URL + serverPort + "/order-one-pc";
            executePut(url, orderDto, headers);

            transaction.commit();
            log.info("Distributed transaction committed: {}", transactionId);

            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Delete Record
    public ResponseStatusDto deleteOrder(OrderDto orderDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting distributed transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String url = BASE_URL + serverPort + "/order-one-pc" + "/" + orderDto.getId();
            executeDelete(url, headers);

            transaction.commit();
            log.info("Distributed transaction committed: {}", transactionId);

            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve All Records
    public List<OrderDto> getOrderListAll() throws CustomException {
        DistributedTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting distributed transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String url = BASE_URL + serverPort + "/order-one-pc/scan-all";
            List<OrderDto> result = executeGet(url, headers, new ParameterizedTypeReference<ApiResponse<List<OrderDto>>>() {});

            transaction.commit();
            log.info("Distributed transaction committed: {}", transactionId);

            return result;
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve Records by Partition Key
    public List<OrderDto> getOrderListByPk(OrderDto orderDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting distributed transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String url = BASE_URL + serverPort + "/order-one-pc/scan-by-pk" + "/" + orderDto.getId();
            List<OrderDto> result = executeGet(url, headers, new ParameterizedTypeReference<ApiResponse<List<OrderDto>>>() {});

            transaction.commit();
            log.info("Distributed transaction committed: {}", transactionId);

            return result;
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }
}
