package com.howie.pharmacy.pharmacy_store.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WardImportRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String locationSlug;

    @NotBlank
    private String wardCode;

    private String provinceCode;

    private String legacyAddress;
}
