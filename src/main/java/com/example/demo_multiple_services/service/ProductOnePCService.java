package com.example.demo_multiple_services.service;

import com.example.demo_multiple_services.model.Product;
import com.example.demo_multiple_services.dto.ProductDto;
import com.example.demo_multiple_services.dto.ResponseStatusDto;
import com.example.demo_multiple_services.exception.CustomException;
import com.example.demo_multiple_services.mapper.ProductMapper;
import com.example.demo_multiple_services.repository.ProductRepository;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.io.Key;
import com.scalar.db.exception.transaction.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

/**
 * One-Phase Commit (1PC) Service for Product
 *
 * This service is designed to participate in distributed transactions managed by a BFF layer.
 * It resumes an existing transaction using the provided transaction ID instead of starting a new one.
 *
 * Key differences from standard service:
 * - All methods accept a transaction ID parameter
 * - Uses manager.resume(transactionId) instead of manager.start()
 * - Does NOT commit the transaction (caller is responsible)
 * - No SQL execution methods (pure CRUD operations only)
 * - Transaction lifecycle is managed by the orchestrating BFF service
 */
@Slf4j
@Service
public class ProductOnePCService {
    DistributedTransactionManager manager;

    @Autowired
    ProductRepository productRepository;

    public ProductOnePCService(DistributedTransactionManager manager) throws InstantiationException, IllegalAccessException {
        this.manager = manager;
    }

    // Create Record
    public ResponseStatusDto insertProduct(ProductDto productDto, String transactionId) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            Product product = ProductMapper.mapToProduct(productDto);
            transaction = manager.resume(transactionId);
            product = productRepository.insertProduct(transaction, product);
            // Note: Do NOT commit - transaction is managed by the caller (BFF)
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Upsert Record
    public ResponseStatusDto upsertProduct(ProductDto productDto, String transactionId) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            Product product = ProductMapper.mapToProduct(productDto);
            transaction = manager.resume(transactionId);
            product = productRepository.upsertProduct(transaction, product);
            // Note: Do NOT commit - transaction is managed by the caller (BFF)
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve Record
    public ProductDto getProduct(ProductDto productDto, String transactionId) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            Product product = ProductMapper.mapToProduct(productDto);
            transaction = manager.resume(transactionId);
            product = productRepository.getProduct(transaction, product);
            // Note: Do NOT commit - transaction is managed by the caller (BFF)
            return ProductMapper.mapToProductDto(product);
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Update Record
    public ResponseStatusDto updateProduct(ProductDto productDto, String transactionId) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            Product product = ProductMapper.mapToProduct(productDto);
            transaction = manager.resume(transactionId);
            product = productRepository.updateProduct(transaction, product);
            // Note: Do NOT commit - transaction is managed by the caller (BFF)
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Delete Record
    public ResponseStatusDto deleteProduct(ProductDto productDto, String transactionId) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            Product product = ProductMapper.mapToProduct(productDto);
            transaction = manager.resume(transactionId);
            productRepository.deleteProduct(transaction, product);
            // Note: Do NOT commit - transaction is managed by the caller (BFF)
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve All Records
    public List<ProductDto> getProductListAll(String transactionId) throws CustomException {
        DistributedTransaction transaction = null;
        List<Product> productList = new ArrayList<>();
        try {
            transaction = manager.resume(transactionId);
            productList = productRepository.getProductListAll(transaction);
            // Note: Do NOT commit - transaction is managed by the caller (BFF)
            return ProductMapper.mapToProductDtoList(productList);
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve Records by Partition Key
    public List<ProductDto> getProductListByPk(ProductDto productDto, String transactionId) throws CustomException {
        DistributedTransaction transaction = null;
        List<Product> productList = new ArrayList<>();
        try {
            Product product = ProductMapper.mapToProduct(productDto);
            Key partitionKey = product.getPartitionKey();
            transaction = manager.resume(transactionId);
            productList = productRepository.getProductListByPk(transaction, partitionKey);
            // Note: Do NOT commit - transaction is managed by the caller (BFF)
            return ProductMapper.mapToProductDtoList(productList);
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    private void handleTransactionException(Exception e, DistributedTransaction transaction) {
        log.error(e.getMessage(), e);
        if (transaction != null) {
            try {
                transaction.rollback();
            } catch (RollbackException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
    }

    private int determineErrorCode(Exception e) {
        if (e instanceof UnsatisfiedConditionException) return 9100;
        if (e instanceof UnknownTransactionStatusException) return 9200;
        if (e instanceof TransactionException) return 9300;
        if (e instanceof RuntimeException) return 9400;
        return 9500;
    }
}
