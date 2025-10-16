package com.example.demo_multiple_services.service;

import com.example.demo_multiple_services.dto.ApiResponse;
import com.example.demo_multiple_services.exception.CustomException;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.exception.transaction.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

/**
 * Base class for One-Phase Commit BFF Services
 *
 * This abstract class provides common functionality for coordinating distributed transactions
 * across multiple 1PC microservices by:
 * - Managing ScalarDB distributed transaction lifecycle
 * - Providing reusable REST API call methods
 * - Handling transaction commits and rollbacks
 * - Centralizing error handling and response validation
 *
 * All OnePC BFF service classes should extend this base class to inherit these capabilities.
 */
@Slf4j
public abstract class BaseOnePCBffService {

    @Autowired
    protected RestTemplate restTemplate;

    protected DistributedTransactionManager manager;

    protected BaseOnePCBffService(DistributedTransactionManager manager) {
        this.manager = manager;
    }

    /**
     * Execute HTTP POST operation for insert/upsert operations
     *
     * @param url The target URL
     * @param dto The DTO object to send in the request body
     * @param headers HTTP headers including transaction ID
     * @param <T> The type of the DTO
     * @throws CustomException if the operation fails
     */
    protected <T> void executePost(String url, T dto, HttpHeaders headers) throws CustomException {
        HttpEntity<T> request = new HttpEntity<>(dto, headers);

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            request,
            new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        ApiResponse<Void> body = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || body == null || !body.isSuccess()) {
            String errorMsg = body != null ? body.getMessage() : "POST operation failed";
            Integer errorCode = body != null ? body.getErrorCode() : 9100;
            throw new CustomException("POST operation failed: " + errorMsg, errorCode != null ? errorCode : 9100);
        }
    }

    /**
     * Execute HTTP PUT operation for update operations
     *
     * @param url The target URL
     * @param dto The DTO object to send in the request body
     * @param headers HTTP headers including transaction ID
     * @param <T> The type of the DTO
     * @throws CustomException if the operation fails
     */
    protected <T> void executePut(String url, T dto, HttpHeaders headers) throws CustomException {
        HttpEntity<T> request = new HttpEntity<>(dto, headers);

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
            url,
            HttpMethod.PUT,
            request,
            new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        ApiResponse<Void> body = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || body == null || !body.isSuccess()) {
            String errorMsg = body != null ? body.getMessage() : "PUT operation failed";
            Integer errorCode = body != null ? body.getErrorCode() : 9100;
            throw new CustomException("PUT operation failed: " + errorMsg, errorCode != null ? errorCode : 9100);
        }
    }

    /**
     * Execute HTTP DELETE operation
     *
     * @param url The target URL (should include path parameters)
     * @param headers HTTP headers including transaction ID
     * @throws CustomException if the operation fails
     */
    protected void executeDelete(String url, HttpHeaders headers) throws CustomException {
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
            url,
            HttpMethod.DELETE,
            request,
            new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        ApiResponse<Void> body = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || body == null || !body.isSuccess()) {
            String errorMsg = body != null ? body.getMessage() : "DELETE operation failed";
            Integer errorCode = body != null ? body.getErrorCode() : 9100;
            throw new CustomException("DELETE operation failed: " + errorMsg, errorCode != null ? errorCode : 9100);
        }
    }

    /**
     * Execute HTTP GET operation that returns data
     *
     * @param url The target URL
     * @param headers HTTP headers including transaction ID
     * @param typeRef ParameterizedTypeReference for the response type
     * @param <T> The type of data returned
     * @return The data from the response
     * @throws CustomException if the operation fails
     */
    protected <T> T executeGet(String url, HttpHeaders headers, ParameterizedTypeReference<ApiResponse<T>> typeRef) throws CustomException {
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<T>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            request,
            typeRef
        );

        ApiResponse<T> body = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || body == null || !body.isSuccess()) {
            String errorMsg = body != null ? body.getMessage() : "GET operation failed";
            Integer errorCode = body != null ? body.getErrorCode() : 9100;
            throw new CustomException("GET operation failed: " + errorMsg, errorCode != null ? errorCode : 9100);
        }

        return body.getData();
    }

    /**
     * Handle transaction exception by rolling back if possible
     *
     * @param e The exception that occurred
     * @param transaction The transaction to rollback
     */
    protected void handleTransactionException(Exception e, DistributedTransaction transaction) {
        log.error(e.getMessage(), e);
        if (transaction != null) {
            try {
                transaction.rollback();
            } catch (RollbackException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Determine error code based on exception type
     *
     * @param e The exception
     * @return The appropriate error code
     */
    protected int determineErrorCode(Exception e) {
        if (e instanceof UnsatisfiedConditionException) return 9100;
        if (e instanceof UnknownTransactionStatusException) return 9200;
        if (e instanceof TransactionException) return 9300;
        if (e instanceof RuntimeException) return 9400;
        return 9500;
    }
}
