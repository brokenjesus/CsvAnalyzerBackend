package by.lupach.backend.controllers;

import by.lupach.backend.dtos.AnalysisResultDTO;
import by.lupach.backend.dtos.FileUploadResponseDTO;
import by.lupach.backend.dtos.PageResponseDTO;
import by.lupach.backend.services.FIleAnalysisHistoryService;
import by.lupach.backend.services.fileprocessing.StreamingFileProcessingService;
import by.lupach.backend.services.files.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class FileController {

    private final FileService fileService;
    private final StreamingFileProcessingService fileProcessingService;
    private final FIleAnalysisHistoryService FIleAnalysisHistoryService;

    @PostMapping("/analyze")
    public ResponseEntity<FileUploadResponseDTO> analyze(@RequestParam("file") MultipartFile file) throws Exception {
        FileUploadResponseDTO fileId = fileService.uploadAndQueue(file);
        return ResponseEntity.ok(fileId);
    }


    @PostMapping("/cancel/{fileId}")
    public ResponseEntity<Void> cancel(@PathVariable UUID fileId) {
        fileProcessingService.cancelProcessing(fileId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history")
    public ResponseEntity<PageResponseDTO<AnalysisResultDTO>> getHistory(
            @RequestParam(defaultValue = "0") int page) {
            return ResponseEntity.ok(FIleAnalysisHistoryService.getHistory(page));
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<AnalysisResultDTO> getAnalysisDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(FIleAnalysisHistoryService.getAnalysisDetailsByFileId(id));
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<Void> deleteAnalysis(@PathVariable UUID id) {
        FIleAnalysisHistoryService.deleteAnalysisByFileId(id);
        return ResponseEntity.ok().build();
    }
}