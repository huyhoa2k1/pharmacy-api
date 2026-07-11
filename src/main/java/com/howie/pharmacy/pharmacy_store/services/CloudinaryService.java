package com.howie.pharmacy.pharmacy_store.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.howie.pharmacy.pharmacy_store.dto.upload.UploadAssetResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public List<String> uploadImages(List<MultipartFile> files, Transformation transformation)
            throws IOException {
        List<String> urls = new ArrayList<>();
        List<Transformation> eagerTransformationsList = Collections.singletonList(transformation);

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                Map<String, Object> options = ObjectUtils.emptyMap(); // Khởi tạo options rỗng

                if (transformation != null) {
                    // Nếu có eager transformations được cung cấp, thêm chúng vào options
                    options = ObjectUtils.asMap("eager", eagerTransformationsList);
                }

                // Thực hiện upload và lấy URL gốc
                Map uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
                List<Map<String, Object>> eagerResults = (List<Map<String, Object>>) uploadResult.get("eager");
                urls.add((String) eagerResults.get(0).get("url"));
            }
        }
        return urls; // Trả về danh sách các URL gốc
    }

    // Phương thức upload cơ bản không có transformation
    public Map uploadImage(MultipartFile file) throws IOException {
        return cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
    }

    public UploadAssetResponseDto uploadImageAsset(MultipartFile file, String folder, String publicId)
            throws IOException {
        Map<String, Object> options = new HashMap<>(ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "image",
                "overwrite", true));

        if (publicId != null && !publicId.isBlank()) {
            String normalizedPublicId = publicId;
            if (publicId.startsWith(folder + "/")) {
                normalizedPublicId = publicId.substring(folder.length() + 1);
            }
            options.put("public_id", normalizedPublicId);
        }

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
        return buildUploadAssetResponse(uploadResult);
    }

    public List<UploadAssetResponseDto> uploadImageAssets(List<MultipartFile> files, String folder,
            List<String> publicIds) throws IOException {
        List<UploadAssetResponseDto> responses = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            if (!file.isEmpty()) {
                String publicId = null;
                if (publicIds != null && publicIds.size() > i) {
                    publicId = publicIds.get(i);
                }
                responses.add(uploadImageAsset(file, folder, publicId));
            }
        }

        return responses;
    }

    private UploadAssetResponseDto buildUploadAssetResponse(Map uploadResult) {
        return new UploadAssetResponseDto(
                (String) uploadResult.get("public_id"),
                (String) uploadResult.get("secure_url"),
                (String) uploadResult.get("url"),
                (String) uploadResult.get("folder"),
                (String) uploadResult.get("format"));
    }
}