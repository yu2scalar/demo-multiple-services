package com.example.demo_multiple_services.repository;

import com.example.demo_multiple_services.model.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.scalar.db.api.*;
import com.scalar.db.exception.transaction.*;
import com.scalar.db.io.Key;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {

    private int scanLimit = 100; // Default scan limit
    
    public void setScanLimit(int scanLimit) {
        this.scanLimit = scanLimit;
    }
    
    public int getScanLimit() {
        return scanLimit;
    }

    // Get Record by Partition & Clustering Key
    public Product getProduct(DistributedTransaction transaction, Product product) throws CrudException {
        Key partitionKey = product.getPartitionKey();
        
        Get get = Get.newBuilder()
            .namespace(Product.NAMESPACE)
            .table(Product.TABLE)
            .partitionKey(partitionKey)
            
            .projections(Product.ID, Product.PRODUCT_NAME, Product.STOCK)
            .build();
        Optional<Result> result = transaction.get(get);
        if (result.isEmpty()) {
            throw new RuntimeException("No record found in Product");
        }
        return buildProduct(result.get());
    }

    // Insert Record
    public Product insertProduct(DistributedTransaction transaction, Product product) throws CrudException {
        Key partitionKey = product.getPartitionKey();
        
        Insert insert = Insert.newBuilder()
            .namespace(Product.NAMESPACE)
            .table(Product.TABLE)
            .partitionKey(partitionKey)
            .textValue(Product.PRODUCT_NAME, product.getProductName())
            .intValue(Product.STOCK, product.getStock())
            .build();
        transaction.insert(insert);
        return product;
    }

    // Update Record
    public Product updateProduct(DistributedTransaction transaction, Product product) throws CrudException {
        Key partitionKey = product.getPartitionKey();
        
        MutationCondition condition = ConditionBuilder.updateIfExists();

        Update update = Update.newBuilder()
            .namespace(Product.NAMESPACE)
            .table(Product.TABLE)
            .partitionKey(partitionKey)
            .textValue(Product.PRODUCT_NAME, product.getProductName())
            .intValue(Product.STOCK, product.getStock())
            .condition(condition)
            .build();
        transaction.update(update);
        return product;
    }

    // Upsert Record
    public Product upsertProduct(DistributedTransaction transaction, Product product) throws CrudException {
        Key partitionKey = product.getPartitionKey();
        
        Upsert upsert = Upsert.newBuilder()
            .namespace(Product.NAMESPACE)
            .table(Product.TABLE)
            .partitionKey(partitionKey)
            .textValue(Product.PRODUCT_NAME, product.getProductName())
            .intValue(Product.STOCK, product.getStock())
            .build();
        transaction.upsert(upsert);
        return product;
    }

    // Delete Record
    public void deleteProduct(DistributedTransaction transaction, Product product) throws CrudException {
        Key partitionKey = product.getPartitionKey();
        
        MutationCondition condition = ConditionBuilder.deleteIfExists();
        Delete delete = Delete.newBuilder()
            .namespace(Product.NAMESPACE)
            .table(Product.TABLE)
            .partitionKey(partitionKey)
            
            .condition(condition)
            .build();
        transaction.delete(delete);
    }

    // Scan All Records
    public List<Product> getProductListAll(DistributedTransaction transaction) throws CrudException {
        Scan scan = Scan.newBuilder()
            .namespace(Product.NAMESPACE)
            .table(Product.TABLE)
            .all()
            .projections(Product.ID, Product.PRODUCT_NAME, Product.STOCK)
            .limit(scanLimit)
            .build();
        List<Result> results = transaction.scan(scan);
        List<Product> productList = new ArrayList<>();
        for (Result result : results) {
            productList.add(buildProduct(result));
        }
        return productList;
    }

    // Scan Records by Partition Key
    public List<Product> getProductListByPk(DistributedTransaction transaction, Key partitionKey) throws CrudException {
        Scan scan = Scan.newBuilder()
            .namespace(Product.NAMESPACE)
            .table(Product.TABLE)
            .partitionKey(partitionKey)
            .projections(Product.ID, Product.PRODUCT_NAME, Product.STOCK)
            .limit(scanLimit)
            .build();
        List<Result> results = transaction.scan(scan);
        List<Product> productList = new ArrayList<>();
        for (Result result : results) {
            productList.add(buildProduct(result));
        }
        return productList;
    }

    // Object Builder from ScalarDB Result
    private Product buildProduct(Result result) {
        return Product.builder()
            .id(result.getInt(Product.ID))
            .productName(result.getText(Product.PRODUCT_NAME))
            .stock(result.getInt(Product.STOCK))
            .build();
    }
}