package by.lupach.backend.controllers;

import by.lupach.backend.dtos.AnalysisResultDTO;
import by.lupach.backend.dtos.PageResponseDTO;
import by.lupach.backend.services.FileService;
import by.lupach.backend.services.HistoryService;
import by.lupach.backend.services.StreamingFileProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final StreamingFileProcessingService fileProcessingService;
    private final HistoryService historyService;

    @PostMapping("/analyze")
    public ResponseEntity<UUID> analyze(@RequestParam("file") MultipartFile file) throws Exception {
        UUID fileId = fileService.uploadAndQueueFile(file);
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
        return ResponseEntity.ok(historyService.getHistory(page));
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<AnalysisResultDTO> getAnalysisDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(historyService.getAnalysisDetails(id));
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<Void> deleteAnalysis(@PathVariable UUID id) {
        historyService.deleteAnalysis(id);
        return ResponseEntity.ok().build();
    }
}