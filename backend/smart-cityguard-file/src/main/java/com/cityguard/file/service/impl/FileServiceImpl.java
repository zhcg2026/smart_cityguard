package com.cityguard.file.service.impl;

import com.cityguard.common.exception.BusinessException;
import com.cityguard.file.config.MinioConfig;
import com.cityguard.file.service.FileService;
import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Override
    public String uploadFile(MultipartFile file, String category) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String fileName = category + "/" + datePath + "/" + UUID.randomUUID().toString() + extension;

            ensureBucketExists();

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );

            return minioConfig.getEndpoint() + "/" + minioConfig.getBucketName() + "/" + fileName;
        } catch (Exception e) {
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public String uploadImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只能上传图片文件");
        }
        return uploadFile(file, "image");
    }

    @Override
    public String uploadVideo(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new BusinessException("只能上传视频文件");
        }
        return uploadFile(file, "video");
    }

    @Override
    public List<String> uploadFiles(List<MultipartFile> files, String category) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(uploadFile(file, category));
        }
        return urls;
    }

    @Override
    public byte[] downloadFile(String fileUrl) {
        try {
            String objectName = extractObjectName(fileUrl);

            InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .build()
            );

            return stream.readAllBytes();
        } catch (Exception e) {
            throw new BusinessException("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        try {
            String objectName = extractObjectName(fileUrl);

            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .build()
            );
            return true;
        } catch (Exception e) {
            throw new BusinessException("文件删除失败: " + e.getMessage());
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
            BucketExistsArgs.builder()
                .bucket(minioConfig.getBucketName())
                .build()
        );

        if (!exists) {
            minioClient.makeBucket(
                MakeBucketArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .build()
            );
        }
    }

    private String extractObjectName(String fileUrl) {
        String bucketName = minioConfig.getBucketName();
        int startIndex = fileUrl.indexOf(bucketName);
        if (startIndex == -1) {
            throw new BusinessException("无效的文件URL");
        }
        return fileUrl.substring(startIndex + bucketName.length() + 1);
    }
}