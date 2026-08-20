package com.aaryan.file_storage_service.controller;

import com.aaryan.file_storage_service.dto.FileResponse;
import com.aaryan.file_storage_service.model.FileMetadata;
import com.aaryan.file_storage_service.repository.FileMetadataRepository;
import com.aaryan.file_storage_service.service.S3Service;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final S3Service s3Service;
    private final FileMetadataRepository fileMetadataRepository;

    public FileController(S3Service s3Service, FileMetadataRepository fileMetadataRepository) {
        this.s3Service = s3Service;
        this.fileMetadataRepository = fileMetadataRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileResponse> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        String s3Key = s3Service.uploadFile(file);

        FileMetadata metadata = new FileMetadata(
                file.getOriginalFilename(),
                s3Key,
                file.getContentType(),
                file.getSize()
        );
        fileMetadataRepository.save(metadata);

        return ResponseEntity.ok(toResponse(metadata));
    }

    @GetMapping
    public ResponseEntity<List<FileResponse>> listFiles() {
        List<FileResponse> files = fileMetadataRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) {
        FileMetadata metadata = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        byte[] data = s3Service.downloadFile(metadata.getS3Key());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getFileName() + "\"")
                .body(data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable Long id) {
        FileMetadata metadata = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        s3Service.deleteFile(metadata.getS3Key());
        fileMetadataRepository.delete(metadata);

        return ResponseEntity.ok("File deleted successfully");
    }

    private FileResponse toResponse(FileMetadata metadata) {
        return new FileResponse(
                metadata.getId(),
                metadata.getFileName(),
                metadata.getContentType(),
                metadata.getFileSize(),
                metadata.getUploadedAt()
        );
    }
}