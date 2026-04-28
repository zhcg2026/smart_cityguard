package com.cityguard.file.controller;

import com.cityguard.file.service.FileService;
import com.cityguard.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "文件管理", description = "文件上传、下载")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public Result<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "common") String category) {
        return Result.success(fileService.uploadFile(file, category));
    }

    @Operation(summary = "上传图片")
    @PostMapping("/upload/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.success(fileService.uploadImage(file));
    }

    @Operation(summary = "上传视频")
    @PostMapping("/upload/video")
    public Result<String> uploadVideo(@RequestParam("file") MultipartFile file) {
        return Result.success(fileService.uploadVideo(file));
    }

    @Operation(summary = "批量上传文件")
    @PostMapping("/upload/batch")
    public Result<List<String>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "category", defaultValue = "common") String category) {
        return Result.success(fileService.uploadFiles(files, category));
    }

    @Operation(summary = "下载文件")
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadFile(@RequestParam String fileUrl) {
        byte[] data = fileService.downloadFile(fileUrl);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"download\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/delete")
    public Result<Boolean> deleteFile(@RequestParam String fileUrl) {
        return Result.success(fileService.deleteFile(fileUrl));
    }
}