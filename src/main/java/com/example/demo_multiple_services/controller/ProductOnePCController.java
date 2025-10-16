package com.example.demo_multiple_services.controller;

import com.example.demo_multiple_services.service.ProductOnePCService;
import com.example.demo_multiple_services.dto.ProductDto;
import com.example.demo_multiple_services.dto.ApiResponse;
import com.example.demo_multiple_services.dto.ResponseStatusDto;
import com.example.demo_multiple_services.exception.CustomException;
import com.scalar.db.exception.transaction.CrudException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * One-Phase Commit (1PC) Controller for Product
 *
 * This controller is designed to participate in distributed transactions managed by a BFF layer.
 * It receives a transaction ID via the ScalarDB-Transaction-ID header and resumes the existing
 * transaction instead of starting a new one.
 *
 * Key differences from standard controller:
 * - All methods accept ScalarDB-Transaction-ID header
 * - Transaction is resumed (not started) in the service layer
 * - Transaction commit is handled by the caller (BFF)
 * - No SQL execution endpoints (pure CRUD operations only)
 */
@RequestMapping(value = "/product-one-pc")
@RestController
public class ProductOnePCController {
    @Autowired
    private ProductOnePCService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> insertProduct(
            @RequestBody ProductDto productDto,
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        ResponseStatusDto status = productService.insertProduct(productDto, transactionId);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @PostMapping("/upsert")
    public ResponseEntity<ApiResponse<Void>> upsertProduct(
            @RequestBody ProductDto productDto,
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        ResponseStatusDto status = productService.upsertProduct(productDto, transactionId);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(
            @PathVariable("id") Integer id,
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        ProductDto productDto = ProductDto.builder()
            .id(id)
            .build();
        ProductDto result = productService.getProduct(productDto, transactionId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateProduct(
            @RequestBody ProductDto productDto,
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        ResponseStatusDto status = productService.updateProduct(productDto, transactionId);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable("id") Integer id,
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        ProductDto productDto = ProductDto.builder()
            .id(id)
            .build();
        ResponseStatusDto status = productService.deleteProduct(productDto, transactionId);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/scan-by-pk/{id}")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getProductByPk(
            @PathVariable("id") Integer id,
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        ProductDto productDto = ProductDto.builder()
            .id(id)
            .build();
        List<ProductDto> result = productService.getProductListByPk(productDto, transactionId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/scan-all")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getProductListAll(
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        List<ProductDto> result = productService.getProductListAll(transactionId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @ExceptionHandler(value = CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleScalarDbException(CustomException ex) {
        ApiResponse<Void> errorResponse = ApiResponse.error(ex.getErrorCode(), ex.getMessage());
        return switch (ex.getErrorCode()) {
            case 9100, 9400 -> new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            case 9200, 9300 -> new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            default -> new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        };
    }
}
