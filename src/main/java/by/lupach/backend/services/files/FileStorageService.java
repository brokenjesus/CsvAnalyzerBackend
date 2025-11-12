package by.lupach.backend.services.files;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
}
