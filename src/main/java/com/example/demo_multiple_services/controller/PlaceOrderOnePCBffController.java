package com.example.demo_multiple_services.controller;

import com.example.demo_multiple_services.dto.ApiResponse;
import com.example.demo_multiple_services.dto.OrderDto;
import com.example.demo_multiple_services.dto.ResponseStatusDto;
import com.example.demo_multiple_services.exception.CustomException;
import com.example.demo_multiple_services.service.OrderOnePCBffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
@RequestMapping(value = "/place-order-one-pc-bff")
@RestController
public class PlaceOrderOnePCBffController {
    @Autowired
    private OrderOnePCBffService orderOnePCBffService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> insertOrder(@RequestBody OrderDto orderDto) {
        ResponseStatusDto status = orderOnePCBffService.placeOrder(orderDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
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
