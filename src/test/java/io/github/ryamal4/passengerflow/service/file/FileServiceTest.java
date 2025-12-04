package io.github.ryamal4.passengerflow.service.file;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {
    private static final String TEST_UPLOAD_DIR = "test-uploads";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService();
        ReflectionTestUtils.setField(fileService, "uploadDir", TEST_UPLOAD_DIR);
    }

    @AfterEach
    void tearDown() throws IOException {
        var uploadPath = Paths.get(TEST_UPLOAD_DIR);
        if (Files.exists(uploadPath)) {
            Files.walk(uploadPath)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                        }
                    });
        }
    }

    @Test
    void testStoreFileSuccess() throws IOException {
        var file = createMockFile("test.jpg", "image/jpeg", 1024, createJpegBytes());

        var filename = fileService.storeFile(file);

        assertThat(filename).isNotNull();
        assertThat(filename).endsWith(".jpg");
        assertThat(Files.exists(Paths.get(TEST_UPLOAD_DIR).resolve(filename))).isTrue();
    }

    @Test
    void testStoreFileEmptyFile() {
        var file = createMockFile("test.jpg", "image/jpeg", 0, new byte[0]);

        assertThatThrownBy(() -> fileService.storeFile(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("empty file");
    }

    @Test
    void testStoreFileExceedsMaxSize() {
        var largeContent = new byte[(int) (MAX_FILE_SIZE + 1)];
        var file = createMockFile("large.jpg", "image/jpeg", MAX_FILE_SIZE + 1, largeContent);

        assertThatThrownBy(() -> fileService.storeFile(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("exceeds maximum limit");
    }

    @Test
    void testStoreFileInvalidFormat() {
        var file = createMockFile("test.exe", "application/x-msdownload", 1024, new byte[1024]);

        assertThatThrownBy(() -> fileService.storeFile(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void testStoreFileInvalidMimeType() {
        var file = createMockFile("test.jpg", "text/plain", 1024, createJpegBytes());

        assertThatThrownBy(() -> fileService.storeFile(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("MIME type does not match");
    }

    @Test
    void testStoreFileInvalidImageBytes() {
        var invalidBytes = "not an image".getBytes();
        var file = createMockFile("test.jpg", "image/jpeg", invalidBytes.length, invalidBytes);

        assertThatThrownBy(() -> fileService.storeFile(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Invalid image file");
    }

    @Test
    void testStorePdfFileSuccess() throws IOException {
        var pdfContent = createPdfBytes();
        var file = createMockFile("document.pdf", "application/pdf", pdfContent.length, pdfContent);

        var filename = fileService.storeFile(file);

        assertThat(filename).isNotNull();
        assertThat(filename).endsWith(".pdf");
        assertThat(Files.exists(Paths.get(TEST_UPLOAD_DIR).resolve(filename))).isTrue();
    }

    @Test
    void testDeleteFileSuccess() throws IOException {
        var file = createMockFile("test.jpg", "image/jpeg", 1024, createJpegBytes());
        var filename = fileService.storeFile(file);

        var deleted = fileService.deleteFile(filename);

        assertThat(deleted).isTrue();
        assertThat(Files.exists(Paths.get(TEST_UPLOAD_DIR).resolve(filename))).isFalse();
    }

    @Test
    void testDeleteFileNotFound() {
        var deleted = fileService.deleteFile("nonexistent.jpg");

        assertThat(deleted).isFalse();
    }

    private MultipartFile createMockFile(String name, String contentType, long size, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }

    private byte[] createJpegBytes() {
        var jpegHeader = new byte[]{(byte) 0xFF, (byte) 0xD8};
        var jpegFooter = new byte[]{(byte) 0xFF, (byte) 0xD9};
        var content = new byte[1024];

        var result = new byte[jpegHeader.length + content.length + jpegFooter.length];
        System.arraycopy(jpegHeader, 0, result, 0, jpegHeader.length);
        System.arraycopy(content, 0, result, jpegHeader.length, content.length);
        System.arraycopy(jpegFooter, 0, result, jpegHeader.length + content.length, jpegFooter.length);

        return result;
    }

    private byte[] createPdfBytes() {
        var pdfHeader = "%PDF-1.4".getBytes();
        var content = new byte[1024];
        var result = new byte[pdfHeader.length + content.length];
        System.arraycopy(pdfHeader, 0, result, 0, pdfHeader.length);
        System.arraycopy(content, 0, result, pdfHeader.length, content.length);
        return result;
    }
}
