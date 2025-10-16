package com.example.demo_multiple_services.service;

import com.example.demo_multiple_services.model.Order;
import com.example.demo_multiple_services.dto.OrderDto;
import com.example.demo_multiple_services.dto.ResponseStatusDto;
import com.example.demo_multiple_services.exception.CustomException;
import com.example.demo_multiple_services.mapper.OrderMapper;
import com.example.demo_multiple_services.repository.OrderRepository;
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
 * One-Phase Commit (1PC) Service for Order
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
public class OrderOnePCService {
    DistributedTransactionManager manager;

    @Autowired
    OrderRepository orderRepository;

    public OrderOnePCService(DistributedTransactionManager manager) throws InstantiationException, IllegalAccessException {
        this.manager = manager;
    }

    // Create Record
    public ResponseStatusDto insertOrder(OrderDto orderDto, String transactionId) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            Order order = OrderMapper.mapToOrder(orderDto);
            transaction = manager.resume(transactionId);
            order = orderRepository.insertOrder(transaction, order);
            // Note: Do NOT commit - transaction is managed by the caller (BFF)
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Upsert Record
    public ResponseStatusDto upsertOrder(OrderDto orderDto, String transactionId) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            Order order = OrderMapper.mapToOrder(orderDto);
            transaction = manager.resume(transactionId);
            order = orderRepository.upsertOrder(transaction, order);
            // Note: Do NOT commit - transaction is managed by the caller (BFF)
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve Record
    public OrderDto getOrder(OrderDto orderDto, String transactionId) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            Order order = OrderMapper.mapToOrder(orderDto);
            transaction = manager.resume(transactionId);
            order = orderRepository.getOrder(transaction, order);
            // Note: Do NOT commit - transaction is managed by the caller (BFF)
            return OrderMapper.mapToOrderDto(order);
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Update Record
    public ResponseStatusDto updateOrder(OrderDto orderDto, String transactionId) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            Order order = OrderMapper.mapToOrder(orderDto);
            transaction = manager.resume(transactionId);
            order = orderRepository.updateOrder(transaction, order);
            // Note: Do NOT commit - transaction is managed by the caller (BFF)
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Delete Record
    public ResponseStatusDto deleteOrder(OrderDto orderDto, String transactionId) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            Order order = OrderMapper.mapToOrder(orderDto);
            transaction = manager.resume(transactionId);
            orderRepository.deleteOrder(transaction, order);
            // Note: Do NOT commit - transaction is managed by the caller (BFF)
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve All Records
    public List<OrderDto> getOrderListAll(String transactionId) throws CustomException {
        DistributedTransaction transaction = null;
        List<Order> orderList = new ArrayList<>();
        try {
            transaction = manager.resume(transactionId);
            orderList = orderRepository.getOrderListAll(transaction);
            // Note: Do NOT commit - transaction is managed by the caller (BFF)
            return OrderMapper.mapToOrderDtoList(orderList);
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve Records by Partition Key
    public List<OrderDto> getOrderListByPk(OrderDto orderDto, String transactionId) throws CustomException {
        DistributedTransaction transaction = null;
        List<Order> orderList = new ArrayList<>();
        try {
            Order order = OrderMapper.mapToOrder(orderDto);
            Key partitionKey = order.getPartitionKey();
            transaction = manager.resume(transactionId);
            orderList = orderRepository.getOrderListByPk(transaction, partitionKey);
            // Note: Do NOT commit - transaction is managed by the caller (BFF)
            return OrderMapper.mapToOrderDtoList(orderList);
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
