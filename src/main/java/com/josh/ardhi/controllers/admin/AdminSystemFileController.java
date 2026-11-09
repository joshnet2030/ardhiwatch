package com.josh.ardhi.controllers.admin;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.models.SystemFile;
import com.josh.ardhi.services.SystemFileService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
@RequestMapping("/admin/system-file")
public class AdminSystemFileController {

    private final SystemFileService systemFileService;

    public AdminSystemFileController(SystemFileService systemFileService) {
        this.systemFileService = systemFileService;
    }

    @GetMapping("/serve")
    public ResponseEntity<Resource> serveFile(@RequestParam("id") UUID id,
                                              @RequestParam(name = "download", defaultValue = "false") boolean download) {
        return buildFileResponse(id, download);
    }

    @GetMapping("/serve/{id}")
    public ResponseEntity<Resource> serveFileByPath(@PathVariable("id") UUID id,
                                                    @RequestParam(name = "download", defaultValue = "false") boolean download) {
        return buildFileResponse(id, download);
    }

    private ResponseEntity<Resource> buildFileResponse(UUID id, boolean download) {
        try {
            SystemFile systemFile = systemFileService.getById(id);
            if (!StringUtils.hasText(systemFile.getFilePath())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File path is missing");
            }

            Path filePath = Paths.get(systemFile.getFilePath()).toAbsolutePath().normalize();
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found on disk");
            }

            Resource resource = new FileSystemResource(filePath);

            MediaType mediaType = MediaType.APPLICATION_PDF;
            if (StringUtils.hasText(systemFile.getFileType())) {
                try {
                    mediaType = MediaType.parseMediaType(systemFile.getFileType());
                } catch (Exception ignored) {
                    mediaType = MediaType.APPLICATION_PDF;
                }
            }

            String filename = StringUtils.hasText(systemFile.getOriginalFileName())
                    ? systemFile.getOriginalFileName()
                    : filePath.getFileName().toString();

            ContentDisposition disposition = (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                    .filename(filename, StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .body(resource);
        } catch (AppException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
