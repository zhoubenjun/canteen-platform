package com.canteen.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Supplier {
    private Integer id;
    private String name;
    private String legalPerson;
    private String contact;
    private String supplyStart;
    private String supplyEnd;
    private String businessLicense;
    private String foodLicense;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
