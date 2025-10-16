package com.example.demo_multiple_services.model;

import lombok.*;
import com.scalar.db.io.Key;
import java.time.*;
import java.nio.ByteBuffer;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    public static final String NAMESPACE = "inventory";
    public static final String TABLE = "product";
    public static final String ID = "id";
    public static final String PRODUCT_NAME = "product_name";
    public static final String STOCK = "stock";

    private Integer id;
    private String productName;
    private Integer stock;

    public Key getPartitionKey() {
        return Key.newBuilder().addInt(ID, getId()).build();
    }

}
