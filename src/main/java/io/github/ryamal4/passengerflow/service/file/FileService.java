package io.github.ryamal4.passengerflow.service.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FileService {

    @Value("${upload.path:uploads}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final Map<String, List<String>> ALLOWED_TYPES = new HashMap<>();
    static {
        ALLOWED_TYPES.put("jpg", Arrays.asList("image/jpeg", "image/jpg"));
        ALLOWED_TYPES.put("jpeg", Arrays.asList("image/jpeg", "image/jpg"));
        ALLOWED_TYPES.put("png", Arrays.asList("image/png"));
        ALLOWED_TYPES.put("gif", Arrays.asList("image/gif"));
        ALLOWED_TYPES.put("bmp", Arrays.asList("image/bmp"));
        ALLOWED_TYPES.put("pdf", Arrays.asList("application/pdf"));
        ALLOWED_TYPES.put("doc", Arrays.asList("application/msword"));
        ALLOWED_TYPES.put("docx", Arrays.asList("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    public String storeFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("File size exceeds maximum limit of 5MB");
        }

        var originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        var extension = getFileExtension(originalFilename);

        if (extension == null || !ALLOWED_TYPES.containsKey(extension.toLowerCase())) {
            throw new IOException("File type not allowed: " + extension);
        }

        var allowedMimeTypes = ALLOWED_TYPES.get(extension.toLowerCase());
        if (!allowedMimeTypes.contains(file.getContentType())) {
            throw new IOException("File MIME type does not match extension");
        }

        var fileBytes = file.getBytes();
        if (extension.toLowerCase().matches("jpg|jpeg|png|gif|bmp") && !isValidImageFile(fileBytes)) {
                throw new IOException("Invalid image file");
            }


        var filename = UUID.randomUUID() + "." + extension;
        var uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        var filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filename;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private boolean isValidImageFile(byte[] fileBytes) {
        if (fileBytes.length < 4) {
            return false;
        }

        if (fileBytes[0] == (byte) 0x89 && fileBytes[1] == (byte) 0x50 &&
                fileBytes[2] == (byte) 0x4E && fileBytes[3] == (byte) 0x47) {
            return true;
        }

        if (fileBytes[0] == (byte) 0xFF && fileBytes[1] == (byte) 0xD8 &&
                fileBytes[fileBytes.length - 2] == (byte) 0xFF &&
                fileBytes[fileBytes.length - 1] == (byte) 0xD9) {
            return true;
        }

        if (fileBytes[0] == (byte) 0x47 && fileBytes[1] == (byte) 0x49 &&
                fileBytes[2] == (byte) 0x46) {
            return true;
        }

        if (fileBytes[0] == (byte) 0x42 && fileBytes[1] == (byte) 0x4D) {
            return true;
        }

        return false;
    }

    public boolean deleteFile(String fileName) {
        try {
            var filePath = Paths.get(uploadDir).resolve(fileName);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }
}
