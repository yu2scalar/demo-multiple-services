package com.example.demo_multiple_services.mapper;

import com.example.demo_multiple_services.model.Product;
import com.example.demo_multiple_services.dto.ProductDto;
import java.util.ArrayList;
import java.util.List;
import org.modelmapper.ModelMapper;

public class ProductMapper {
    private static final ModelMapper modelMapper = new ModelMapper();

    // Convert Model to DTO
    public static ProductDto mapToProductDto(Product product) {
        return modelMapper.map(product, ProductDto.class);
    }

    // Convert DTO to Model
    public static Product mapToProduct(ProductDto productDto) {
        return modelMapper.map(productDto, Product.class);
    }

    // Convert Model List to DTO List
    public static List<ProductDto> mapToProductDtoList(List<Product> productList) {
        List<ProductDto> productDtoList = new ArrayList<>();
        for (Product product : productList) {
            productDtoList.add(mapToProductDto(product));
        }
        return productDtoList;
    }
}