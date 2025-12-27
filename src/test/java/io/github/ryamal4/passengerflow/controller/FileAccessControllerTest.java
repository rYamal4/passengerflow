package io.github.ryamal4.passengerflow.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileAccessController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "upload.path=test-uploads")
class FileAccessControllerTest extends AbstractControllerTest {

    private static final String BASE_URL = "/files";
    private static final Path TEST_UPLOAD_DIR = Path.of("test-uploads");

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(TEST_UPLOAD_DIR);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(TEST_UPLOAD_DIR)) {
            try (var paths = Files.walk(TEST_UPLOAD_DIR)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException ignored) {
                                // ignore
                            }
                        });
            }
        }
    }

    @Test
    void testServeFileSuccess() throws Exception {
        var testFile = TEST_UPLOAD_DIR.resolve("test.txt");
        Files.writeString(testFile, "test content");

        mockMvc.perform(get(BASE_URL + "/test.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.txt\""));
    }

    @Test
    void testServeFileNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/nonexistent.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testServeFileWithDifferentExtensions() throws Exception {
        var testFile = TEST_UPLOAD_DIR.resolve("document.pdf");
        Files.write(testFile, new byte[]{0x25, 0x50, 0x44, 0x46});

        mockMvc.perform(get(BASE_URL + "/document.pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"document.pdf\""));
    }

    @Test
    void testServeFileWithSpecialCharacters() throws Exception {
        var testFile = TEST_UPLOAD_DIR.resolve("file-name_123.txt");
        Files.writeString(testFile, "content");

        mockMvc.perform(get(BASE_URL + "/file-name_123.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"file-name_123.txt\""));
    }

    @Test
    void testServeFileReturnsOctetStreamContentType() throws Exception {
        var testFile = TEST_UPLOAD_DIR.resolve("image.png");
        Files.write(testFile, new byte[]{0x00, 0x01, 0x02});

        mockMvc.perform(get(BASE_URL + "/image.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/octet-stream"));
    }
}
