package by.lupach.backend.services.files;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public Path saveFile(MultipartFile file, String filePath) throws IOException {
        Path path = Paths.get(uploadDir, filePath);
        Files.createDirectories(path.getParent());
        file.transferTo(path);
        return path;
    }

    public void deletePhysicalFileViaInternalPath(String internalFilePath) {
        Path path = Paths.get(uploadDir, internalFilePath);

        try {
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                log.info("Physical file deleted for analysis: {}", path);
            } else {
                log.warn("Physical file not found for deletion: {}", path);
            }
        } catch (IOException e) {
            log.error("Failed to delete physical file for analysis: {}", path, e);
        }
    }
}
