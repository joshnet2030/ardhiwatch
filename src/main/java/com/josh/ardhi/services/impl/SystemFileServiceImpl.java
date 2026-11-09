package com.josh.ardhi.services.impl;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.models.SystemFile;
import com.josh.ardhi.repositories.SystemFileRepository;
import com.josh.ardhi.services.SystemFileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class SystemFileServiceImpl implements SystemFileService {

    private final SystemFileRepository systemFileRepository;
    private final String uploadDir;

    public SystemFileServiceImpl(SystemFileRepository systemFileRepository,
                                 @Value("${upload.dir}") String uploadDir) {
        this.systemFileRepository = systemFileRepository;
        this.uploadDir = uploadDir;
    }

    @Override
    public SystemFile store(MultipartFile file) throws AppException {
        if (file == null || file.isEmpty()) {
            throw new AppException("Uploaded file must not be empty");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "gazette.pdf"
        );

        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex);
        }

        String generatedFileName = UUID.randomUUID() + extension;

        try {
            Path rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(rootLocation);

            Path destinationFile = rootLocation.resolve(generatedFileName).normalize();
            if (!destinationFile.getParent().equals(rootLocation)) {
                throw new AppException("Cannot store file outside configured upload directory");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            SystemFile systemFile = SystemFile.builder()
                    .originalFileName(originalFilename)
                    .fileType(file.getContentType())
                    .sizeInBytes(file.getSize())
                    .generatedFileName(generatedFileName)
                    .filePath(destinationFile.toString())
                    .build();

            return systemFileRepository.save(systemFile);
        } catch (IOException e) {
            throw new AppException("Failed to store uploaded file: " + e.getMessage());
        }
    }

    @Override
    public SystemFile storeBytes(byte[] content, String originalFileName, String contentType) throws AppException {
        if (content == null || content.length == 0) {
            throw new AppException("File content must not be empty");
        }

        String cleanedName = StringUtils.cleanPath(
                StringUtils.hasText(originalFileName) ? originalFileName : "gazette-page.pdf"
        );

        String extension = "";
        int dotIndex = cleanedName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = cleanedName.substring(dotIndex);
        }

        String generatedFileName = UUID.randomUUID() + extension;

        try {
            Path rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(rootLocation);

            Path destinationFile = rootLocation.resolve(generatedFileName).normalize();
            if (!destinationFile.getParent().equals(rootLocation)) {
                throw new AppException("Cannot store file outside configured upload directory");
            }

            Files.write(destinationFile, content);

            SystemFile systemFile = SystemFile.builder()
                    .originalFileName(cleanedName)
                    .fileType(StringUtils.hasText(contentType) ? contentType : "application/pdf")
                    .sizeInBytes((long) content.length)
                    .generatedFileName(generatedFileName)
                    .filePath(destinationFile.toString())
                    .build();

            return systemFileRepository.save(systemFile);
        } catch (IOException e) {
            throw new AppException("Failed to store file: " + e.getMessage());
        }
    }

    @Override
    public SystemFile getById(UUID id) throws AppException {
        return systemFileRepository.findById(id)
                .orElseThrow(() -> new AppException("System file not found: " + id));
    }
}
