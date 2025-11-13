package by.lupach.backend.controllers;

import by.lupach.backend.dtos.AnalysisResultDTO;
import by.lupach.backend.dtos.FileUploadResponseDTO;
import by.lupach.backend.dtos.PageResponseDTO;
import by.lupach.backend.services.FileAnalysisService;
import by.lupach.backend.services.HistoryService;
import by.lupach.backend.services.fileprocessing.StreamingFileProcessingService;
import by.lupach.backend.services.files.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final StreamingFileProcessingService fileProcessingService;
    private final HistoryService historyService;
    private final FileAnalysisService fileAnalysisService;

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
    public PageResponseDTO<AnalysisResultDTO> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return historyService.getHistory(page, size);
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<AnalysisResultDTO> getAnalysisDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(fileAnalysisService.getAnalysisDetailsByFileId(id));
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<Void> deleteAnalysis(@PathVariable UUID id) {
        fileAnalysisService.deleteAnalysisByFileId(id);
        return ResponseEntity.ok().build();
    }
}