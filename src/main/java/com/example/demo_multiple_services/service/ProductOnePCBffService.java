package com.example.demo_multiple_services.service;

import com.example.demo_multiple_services.model.Product;
import com.example.demo_multiple_services.dto.ProductDto;
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
 * One-Phase Commit BFF Service for Product
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
public class ProductOnePCBffService extends BaseOnePCBffService {

    @Value("${server.port:8080}")
    private String serverPort;

    private static final String BASE_URL = "http://localhost:";

    public ProductOnePCBffService(DistributedTransactionManager manager) throws InstantiationException, IllegalAccessException {
        super(manager);
    }

    // Create Record
    public ResponseStatusDto insertProduct(ProductDto productDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting distributed transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String url = BASE_URL + serverPort + "/product-one-pc";
            executePost(url, productDto, headers);

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
    public ResponseStatusDto upsertProduct(ProductDto productDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting distributed transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String url = BASE_URL + serverPort + "/product-one-pc/upsert";
            executePost(url, productDto, headers);

            transaction.commit();
            log.info("Distributed transaction committed: {}", transactionId);

            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve Record
    public ProductDto getProduct(ProductDto productDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting distributed transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String url = BASE_URL + serverPort + "/product-one-pc" + "/" + productDto.getId();
            ProductDto result = executeGet(url, headers, new ParameterizedTypeReference<ApiResponse<ProductDto>>() {});

            transaction.commit();
            log.info("Distributed transaction committed: {}", transactionId);

            return result;
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Update Record
    public ResponseStatusDto updateProduct(ProductDto productDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting distributed transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String url = BASE_URL + serverPort + "/product-one-pc";
            executePut(url, productDto, headers);

            transaction.commit();
            log.info("Distributed transaction committed: {}", transactionId);

            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Delete Record
    public ResponseStatusDto deleteProduct(ProductDto productDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting distributed transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String url = BASE_URL + serverPort + "/product-one-pc" + "/" + productDto.getId();
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
    public List<ProductDto> getProductListAll() throws CustomException {
        DistributedTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting distributed transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String url = BASE_URL + serverPort + "/product-one-pc/scan-all";
            List<ProductDto> result = executeGet(url, headers, new ParameterizedTypeReference<ApiResponse<List<ProductDto>>>() {});

            transaction.commit();
            log.info("Distributed transaction committed: {}", transactionId);

            return result;
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve Records by Partition Key
    public List<ProductDto> getProductListByPk(ProductDto productDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting distributed transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String url = BASE_URL + serverPort + "/product-one-pc/scan-by-pk" + "/" + productDto.getId();
            List<ProductDto> result = executeGet(url, headers, new ParameterizedTypeReference<ApiResponse<List<ProductDto>>>() {});

            transaction.commit();
            log.info("Distributed transaction committed: {}", transactionId);

            return result;
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }
}
