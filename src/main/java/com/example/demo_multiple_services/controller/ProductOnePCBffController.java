package com.example.demo_multiple_services.controller;

import com.example.demo_multiple_services.service.ProductOnePCBffService;
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
 * One-Phase Commit BFF Controller for Product
 *
 * This BFF controller coordinates distributed transactions across multiple 1PC microservices.
 * It starts a transaction, calls 1PC endpoints with transaction ID propagation, and commits/rolls back.
 *
 * Key differences from standard controller:
 * - Orchestrates calls to multiple 1PC services
 * - Manages transaction lifecycle (start, propagate, commit/rollback)
 * - No SQL execution endpoints
 */
@RequestMapping(value = "/product-one-pc-bff")
@RestController
public class ProductOnePCBffController {
    @Autowired
    private ProductOnePCBffService productOnePCBffService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> insertProduct(@RequestBody ProductDto productDto) {
        ResponseStatusDto status = productOnePCBffService.insertProduct(productDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @PostMapping("/upsert")
    public ResponseEntity<ApiResponse<Void>> upsertProduct(@RequestBody ProductDto productDto) {
        ResponseStatusDto status = productOnePCBffService.upsertProduct(productDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(@PathVariable("id") Integer id) {
        ProductDto productDto = ProductDto.builder()
            .id(id)
            .build();
        ProductDto result = productOnePCBffService.getProduct(productDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateProduct(@RequestBody ProductDto productDto) {
        ResponseStatusDto status = productOnePCBffService.updateProduct(productDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable("id") Integer id) {
        ProductDto productDto = ProductDto.builder()
            .id(id)
            .build();
        ResponseStatusDto status = productOnePCBffService.deleteProduct(productDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/scan-by-pk/{id}")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getProductByPk(@PathVariable("id") Integer id) {
        ProductDto productDto = ProductDto.builder()
            .id(id)
            .build();
        List<ProductDto> result = productOnePCBffService.getProductListByPk(productDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/scan-all")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getProductListAll() {
        List<ProductDto> result = productOnePCBffService.getProductListAll();
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
