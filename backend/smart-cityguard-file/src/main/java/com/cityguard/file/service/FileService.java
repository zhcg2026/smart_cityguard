package com.cityguard.file.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    String uploadFile(MultipartFile file, String category);

    String uploadImage(MultipartFile file);

    String uploadVideo(MultipartFile file);

    List<String> uploadFiles(List<MultipartFile> files, String category);

    byte[] downloadFile(String fileUrl);

    boolean deleteFile(String fileUrl);
}