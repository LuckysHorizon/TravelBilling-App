package com.travelbillpro.service;

import com.travelbillpro.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Slf4j
public class LocalFileStorageService {

    @Value("${app.storage.base-path}")
    private String basePath;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(basePath);
        try {
            Files.createDirectories(rootLocation);
            Files.createDirectories(rootLocation.resolve("tickets"));
            Files.createDirectories(rootLocation.resolve("invoices"));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directories", e);
        }
    }

    /**
     * Stores a ticket file in the format: tickets/{companyId}/{year}/{month}/{uuid}_{filename}
     * Returns the relative path to be stored in the DB.
     */
    public String storeTicketContent(Long companyId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("Failed to store empty file", "INVALID_FILE", HttpStatus.BAD_REQUEST);
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown");
        if (originalFilename.contains("..")) {
            throw new BusinessException("Cannot store file with relative path outside current directory", "INVALID_FILE_NAME", HttpStatus.BAD_REQUEST);
        }

        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        
        String relativeDir = "tickets/" + companyId + "/" + year + "/" + month;
        Path targetDir = rootLocation.resolve(relativeDir);
        
        try {
            Files.createDirectories(targetDir);
            
            String storedFilename = UUID.randomUUID().toString() + "_" + originalFilename;
            Path targetFile = targetDir.resolve(storedFilename);
            
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            
            return relativeDir + "/" + storedFilename;
            
        } catch (IOException e) {
            log.error("Failed to store file {}", originalFilename, e);
            throw new BusinessException("Failed to store file", "STORAGE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Stores an invoice file (PDF or Excel)
     * Format: invoices/{companyId}/{year}/{invoiceNumber}.{ext}
     */
    public String storeInvoiceFile(Long companyId, String invoiceNumber, byte[] content, String extension) {
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        
        String relativeDir = "invoices/" + companyId + "/" + year;
        Path targetDir = rootLocation.resolve(relativeDir);
        
        try {
            Files.createDirectories(targetDir);
            
            String safeInvoiceNumber = invoiceNumber.replace("/", "-");
            String storedFilename = safeInvoiceNumber + "." + extension;
            Path targetFile = targetDir.resolve(storedFilename);
            
            Files.write(targetFile, content);
            
            return relativeDir + "/" + storedFilename;
            
        } catch (IOException e) {
            log.error("Failed to store invoice file {}", invoiceNumber, e);
            throw new BusinessException("Failed to store invoice", "STORAGE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Resource loadFileAsResource(String relativePath) {
        try {
            Path file = rootLocation.resolve(relativePath).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new BusinessException("Cannot read file outside storage directory", "SECURITY_ERROR", HttpStatus.FORBIDDEN);
            }
            
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new BusinessException("Could not read file", "FILE_NOT_FOUND", HttpStatus.NOT_FOUND);
            }
        } catch (MalformedURLException e) {
            throw new BusinessException("Could not read file", "FILE_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
    }
    
    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) return;
        
        try {
            Path file = rootLocation.resolve(relativePath).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new BusinessException("Cannot delete file outside storage directory", "SECURITY_ERROR", HttpStatus.FORBIDDEN);
            }
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", relativePath, e);
        }
    }
}
