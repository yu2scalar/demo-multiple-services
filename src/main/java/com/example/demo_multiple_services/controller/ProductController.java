package com.example.demo_multiple_services.controller;

import com.example.demo_multiple_services.service.ProductService;
import com.example.demo_multiple_services.dto.ProductDto;
import com.example.demo_multiple_services.dto.ApiResponse;
import com.example.demo_multiple_services.dto.ResponseStatusDto;
import com.example.demo_multiple_services.dto.SqlCommandDto;
import com.example.demo_multiple_services.exception.CustomException;
import com.scalar.db.exception.transaction.CrudException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@RequestMapping(value = "/product")
@RestController
public class ProductController {
    @Autowired
    private ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> insertProduct(@RequestBody ProductDto productDto) {
        ResponseStatusDto status = productService.insertProduct(productDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @PostMapping("/upsert")
    public ResponseEntity<ApiResponse<Void>> upsertProduct(@RequestBody ProductDto productDto) {
        ResponseStatusDto status = productService.upsertProduct(productDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(@PathVariable("id") Integer id) {
        ProductDto productDto = ProductDto.builder()
            .id(id)
            .build();
        ProductDto result = productService.getProduct(productDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateProduct(@RequestBody ProductDto productDto) {
        ResponseStatusDto status = productService.updateProduct(productDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable("id") Integer id) {
        ProductDto productDto = ProductDto.builder()
            .id(id)
            .build();
        ResponseStatusDto status = productService.deleteProduct(productDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/scan-by-pk/{id}")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getProductByPk(@PathVariable("id") Integer id) {
        ProductDto productDto = ProductDto.builder()
            .id(id)
            .build();
        List<ProductDto> result = productService.getProductListByPk(productDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/scan-all")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getProductListAll() {
        List<ProductDto> result = productService.getProductListAll();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/execute-sql")
    public ResponseEntity<ApiResponse<List<ProductDto>>> executeSQL(@RequestBody SqlCommandDto sqlCommandDto) {
        List<ProductDto> result = productService.executeSQL(sqlCommandDto);
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