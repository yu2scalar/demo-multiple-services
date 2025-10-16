package com.example.demo_multiple_services.controller;

import com.example.demo_multiple_services.service.OrderOnePCService;
import com.example.demo_multiple_services.dto.OrderDto;
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
 * One-Phase Commit (1PC) Controller for Order
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
@RequestMapping(value = "/order-one-pc")
@RestController
public class OrderOnePCController {
    @Autowired
    private OrderOnePCService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> insertOrder(
            @RequestBody OrderDto orderDto,
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        ResponseStatusDto status = orderService.insertOrder(orderDto, transactionId);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @PostMapping("/upsert")
    public ResponseEntity<ApiResponse<Void>> upsertOrder(
            @RequestBody OrderDto orderDto,
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        ResponseStatusDto status = orderService.upsertOrder(orderDto, transactionId);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrder(
            @PathVariable("id") String id,
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        OrderDto orderDto = OrderDto.builder()
            .id(id)
            .build();
        OrderDto result = orderService.getOrder(orderDto, transactionId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateOrder(
            @RequestBody OrderDto orderDto,
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        ResponseStatusDto status = orderService.updateOrder(orderDto, transactionId);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(
            @PathVariable("id") String id,
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        OrderDto orderDto = OrderDto.builder()
            .id(id)
            .build();
        ResponseStatusDto status = orderService.deleteOrder(orderDto, transactionId);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/scan-by-pk/{id}")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getOrderByPk(
            @PathVariable("id") String id,
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        OrderDto orderDto = OrderDto.builder()
            .id(id)
            .build();
        List<OrderDto> result = orderService.getOrderListByPk(orderDto, transactionId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/scan-all")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getOrderListAll(
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        List<OrderDto> result = orderService.getOrderListAll(transactionId);
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
