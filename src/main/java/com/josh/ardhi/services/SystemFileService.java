package com.josh.ardhi.services;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.models.SystemFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface SystemFileService {

    SystemFile store(MultipartFile file) throws AppException;

    SystemFile storeBytes(byte[] content, String originalFileName, String contentType) throws AppException;

    SystemFile getById(UUID id) throws AppException;

}
