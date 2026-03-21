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

import com.travelbillpro.entity.Company;
import com.travelbillpro.repository.CompanyRepository;

@Service
@Slf4j
@lombok.RequiredArgsConstructor
public class LocalFileStorageService {

    private final CompanyRepository companyRepository;

    @Value("${app.storage.base-path}")
    private String basePath;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(basePath).toAbsolutePath().normalize();
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
     * Returns the RELATIVE path to be stored in the DB (always relative to rootLocation for consistency).
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
        
        // Check if company has a custom PDF storage path
        String customPath = null;
        try {
            Company company = companyRepository.findById(companyId).orElse(null);
            if (company != null && company.getPdfStoragePath() != null && !company.getPdfStoragePath().isBlank()) {
                customPath = company.getPdfStoragePath();
            }
        } catch (Exception e) {
            log.warn("Could not fetch company {} for custom path lookup: {}", companyId, e.getMessage());
        }

        String storedFilename = UUID.randomUUID().toString() + "_" + originalFilename;
        Path targetDir;
        String finalStoredPath;

        if (customPath != null) {
            // Use custom company storage path — store absolute path in DB
            targetDir = Paths.get(customPath).resolve(year).resolve(month);
            finalStoredPath = targetDir.resolve(storedFilename).toAbsolutePath().toString();
        } else {
            // Use default relative storage
            String relativeDir = "tickets/" + companyId + "/" + year + "/" + month;
            targetDir = rootLocation.resolve(relativeDir);
            finalStoredPath = relativeDir + "/" + storedFilename;
        }
        
        try {
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(storedFilename);
            
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file: {} at {}", originalFilename, targetFile);
            
            return finalStoredPath;
            
        } catch (IOException e) {
            log.error("Failed to store file {}", originalFilename, e);
            throw new BusinessException("Failed to store file", "STORAGE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Stores an invoice file (PDF or Excel)
     * Format: invoices/{companyId}/{year}/{invoiceNumber}.{ext}
     * Returns RELATIVE path for consistency.
     */
    public String storeInvoiceFile(Long companyId, String invoiceNumber, byte[] content, String extension) {
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        
        // ALWAYS use the default storage location for consistency
        String relativeDir = "invoices/" + companyId + "/" + year;
        Path targetDir = rootLocation.resolve(relativeDir);
        String safeInvoiceNumber = invoiceNumber.replace("/", "-");
        String storedFilename = safeInvoiceNumber + "." + extension;
        String finalStoredPath = relativeDir + "/" + storedFilename;
        
        try {
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(storedFilename);
            
            Files.write(targetFile, content);
            log.info("Stored invoice file: {} at {}", invoiceNumber, targetFile);
            
            return finalStoredPath;
            
        } catch (IOException e) {
            log.error("Failed to store invoice file {}", invoiceNumber, e);
            throw new BusinessException("Failed to store invoice", "STORAGE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Resource loadFileAsResource(String relativePath) {
        try {
            Path file;
            Path givenPath = Paths.get(relativePath);
            if (givenPath.isAbsolute()) {
                file = givenPath.normalize();
                // We'll assume the path in DB is legitimate. Alternatively, verify it still exists.
            } else {
                if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);
                file = rootLocation.resolve(relativePath).normalize().toAbsolutePath();
                if (!file.startsWith(rootLocation)) {
                    throw new BusinessException("Cannot read file outside storage directory", "SECURITY_ERROR", HttpStatus.FORBIDDEN);
                }
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
            Path file;
            Path givenPath = Paths.get(relativePath);
            if (givenPath.isAbsolute()) {
                file = givenPath.normalize();
            } else {
                if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);
                file = rootLocation.resolve(relativePath).normalize().toAbsolutePath();
                if (!file.startsWith(rootLocation)) {
                    throw new BusinessException("Cannot delete file outside storage directory", "SECURITY_ERROR", HttpStatus.FORBIDDEN);
                }
            }
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", relativePath, e);
        }
    }
    /**
     * Resolve a stored file path to its absolute path on disk.
     * If the stored path is already absolute (custom company storage), return as-is.
     * Otherwise, resolve relative to the rootLocation.
     * Python extraction service needs the absolute path to read the file.
     */
    public String resolveAbsolutePath(String storedPath) {
        Path givenPath = Paths.get(storedPath);
        if (givenPath.isAbsolute()) {
            return givenPath.normalize().toAbsolutePath().toString();
        }
        return rootLocation.resolve(storedPath).normalize().toAbsolutePath().toString();
    }
}
