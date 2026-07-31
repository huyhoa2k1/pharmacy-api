package com.howie.pharmacy.pharmacy_store.controllers;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.howie.pharmacy.pharmacy_store.dto.upload.UploadAssetResponseDto;
import com.howie.pharmacy.pharmacy_store.services.CloudinaryService;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final CloudinaryService cloudinaryService;

    public AssetController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping("/upload-logo")
    public UploadAssetResponseDto uploadLogo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "publicId", required = false) String publicId)
            throws IOException {
        return cloudinaryService.uploadImageAsset(file, "logo", publicId);
    }

    @PostMapping("/upload-banners")
    public List<UploadAssetResponseDto> uploadBanners(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "publicIds", required = false) List<String> publicIds)
            throws IOException {
        return cloudinaryService.uploadImageAssets(files, "banner", publicIds);
    }
}
