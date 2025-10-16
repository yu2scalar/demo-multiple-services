package com.example.demo_multiple_services.controller;

import com.example.demo_multiple_services.service.OrderOnePCBffService;
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
 * One-Phase Commit BFF Controller for Order
 *
 * This BFF controller coordinates distributed transactions across multiple 1PC microservices.
 * It starts a transaction, calls 1PC endpoints with transaction ID propagation, and commits/rolls back.
 *
 * Key differences from standard controller:
 * - Orchestrates calls to multiple 1PC services
 * - Manages transaction lifecycle (start, propagate, commit/rollback)
 * - No SQL execution endpoints
 */
@RequestMapping(value = "/order-one-pc-bff")
@RestController
public class OrderOnePCBffController {
    @Autowired
    private OrderOnePCBffService orderOnePCBffService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> insertOrder(@RequestBody OrderDto orderDto) {
        ResponseStatusDto status = orderOnePCBffService.insertOrder(orderDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @PostMapping("/upsert")
    public ResponseEntity<ApiResponse<Void>> upsertOrder(@RequestBody OrderDto orderDto) {
        ResponseStatusDto status = orderOnePCBffService.upsertOrder(orderDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrder(@PathVariable("id") String id) {
        OrderDto orderDto = OrderDto.builder()
            .id(id)
            .build();
        OrderDto result = orderOnePCBffService.getOrder(orderDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateOrder(@RequestBody OrderDto orderDto) {
        ResponseStatusDto status = orderOnePCBffService.updateOrder(orderDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable("id") String id) {
        OrderDto orderDto = OrderDto.builder()
            .id(id)
            .build();
        ResponseStatusDto status = orderOnePCBffService.deleteOrder(orderDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/scan-by-pk/{id}")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getOrderByPk(@PathVariable("id") String id) {
        OrderDto orderDto = OrderDto.builder()
            .id(id)
            .build();
        List<OrderDto> result = orderOnePCBffService.getOrderListByPk(orderDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/scan-all")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getOrderListAll() {
        List<OrderDto> result = orderOnePCBffService.getOrderListAll();
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
