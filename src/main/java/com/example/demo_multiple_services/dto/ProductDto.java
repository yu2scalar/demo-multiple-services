package com.example.demo_multiple_services.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.nio.ByteBuffer;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ProductDto {
    private Integer id;
    private String productName;
    private Integer stock;
}