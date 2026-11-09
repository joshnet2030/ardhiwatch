package com.josh.ardhi.services;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.models.SystemFile;
import org.springframework.web.multipart.MultipartFile;

public interface SystemFileService {

    SystemFile store(MultipartFile file) throws AppException;

}
