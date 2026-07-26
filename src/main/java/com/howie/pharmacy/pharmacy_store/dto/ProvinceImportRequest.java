package com.howie.pharmacy.pharmacy_store.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProvinceImportRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String locationSlug;

    @NotBlank
    private String provinceCode;

    private String legacyAddress;

    private List<WardImportRequest> wards;
}
