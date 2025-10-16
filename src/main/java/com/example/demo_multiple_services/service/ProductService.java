package com.example.demo_multiple_services.service;

import com.example.demo_multiple_services.model.Product;
import com.example.demo_multiple_services.dto.ProductDto;
import com.example.demo_multiple_services.dto.ResponseStatusDto;
import com.example.demo_multiple_services.dto.SqlCommandDto;
import com.example.demo_multiple_services.exception.CustomException;
import com.example.demo_multiple_services.mapper.ProductMapper;
import com.example.demo_multiple_services.repository.ProductRepository;
import com.example.demo_multiple_services.util.ExecuteSqlUtil;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.io.Key;
import com.scalar.db.exception.transaction.*;
import com.scalar.db.sql.ResultSet;
import com.scalar.db.sql.SqlSession;
import com.scalar.db.sql.SqlSessionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ProductService {
    DistributedTransactionManager manager;
    SqlSessionFactory sqlSessionFactory;

    @Autowired
    ProductRepository productRepository;

    public ProductService(DistributedTransactionManager manager, SqlSessionFactory sqlSessionFactory) throws InstantiationException, IllegalAccessException {
        this.manager = manager;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    // Execute SQL Command
    public List<ProductDto> executeSQL(SqlCommandDto sqlCommandDto) throws CustomException {
        SqlSession sqlSession = null;

        try {
            sqlSession = sqlSessionFactory.createSqlSession();
            String sqlCommand = sqlCommandDto.getSqlCommand();

            // Begin a transaction
            sqlSession.begin();

            List<ProductDto> result;
            if (isDmlOperation(sqlCommand)) {
                // Handle DML operations (INSERT, UPDATE, DELETE)
                ResultSet resultSet = sqlSession.execute(sqlCommand);
                // For DML operations, return empty list but operation was successful
                result = new ArrayList<>();
            } else {
                // Handle SELECT operations
                ExecuteSqlUtil<Product> executeSql = new ExecuteSqlUtil<>(Product.class);
                List<Product> productList = executeSql.executeSQL(sqlSession, sqlCommand);
                result = ProductMapper.mapToProductDtoList(productList);
            }

            sqlSession.commit();
            return result;
        } catch (Exception e) {
            handleSqlSessionException(e, sqlSession);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Helper method to detect DML operations
    private boolean isDmlOperation(String sqlCommand) {
        String trimmedCommand = sqlCommand.trim().toUpperCase();
        return trimmedCommand.startsWith("INSERT") || 
               trimmedCommand.startsWith("UPDATE") || 
               trimmedCommand.startsWith("DELETE");
    }

    // Create Record
    public ResponseStatusDto insertProduct(ProductDto productDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            Product product = ProductMapper.mapToProduct(productDto);
            transaction = manager.start();
            product = productRepository.insertProduct(transaction, product);
            transaction.commit();
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Upsert Record
    public ResponseStatusDto upsertProduct(ProductDto productDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            Product product = ProductMapper.mapToProduct(productDto);
            transaction = manager.start();
            product = productRepository.upsertProduct(transaction, product);
            transaction.commit();
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
            Product product = ProductMapper.mapToProduct(productDto);
            transaction = manager.start();
            product = productRepository.getProduct(transaction, product);
            transaction.commit();
            return ProductMapper.mapToProductDto(product);
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Update Record
    public ResponseStatusDto updateProduct(ProductDto productDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            Product product = ProductMapper.mapToProduct(productDto);
            transaction = manager.start();
            product = productRepository.updateProduct(transaction, product);
            transaction.commit();
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
            Product product = ProductMapper.mapToProduct(productDto);
            transaction = manager.start();
            productRepository.deleteProduct(transaction, product);
            transaction.commit();
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve All Records
    public List<ProductDto> getProductListAll() throws CustomException {
        DistributedTransaction transaction = null;
        List<Product> productList = new ArrayList<>();
        try {
            transaction = manager.start();
            productList = productRepository.getProductListAll(transaction);
            transaction.commit();
            return ProductMapper.mapToProductDtoList(productList);
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve Records by Partition Key
    public List<ProductDto> getProductListByPk(ProductDto productDto) throws CustomException {
        DistributedTransaction transaction = null;
        List<Product> productList = new ArrayList<>();
        try {
            Product product = ProductMapper.mapToProduct(productDto);
            Key partitionKey = product.getPartitionKey();
            transaction = manager.start();
            productList = productRepository.getProductListByPk(transaction, partitionKey);
            transaction.commit();
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

    private void handleSqlSessionException(Exception e, SqlSession sqlSession) {
        log.error(e.getMessage(), e);
        if (sqlSession != null) {
            try {
                sqlSession.rollback();
            } catch (Exception ex) {
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