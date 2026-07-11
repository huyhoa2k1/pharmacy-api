package com.howie.pharmacy.pharmacy_store.dto.upload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UploadAssetResponseDto {
    private String publicId;
    private String secureUrl;
    private String fileUrl;
    private String folder;
    private String format;
}
