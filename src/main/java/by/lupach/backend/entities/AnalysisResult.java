package by.lupach.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "analysis_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private FileEntity file;

    private LocalDateTime processStartTime;
    private LocalDateTime processEndTime;

    // Композиция - статистика в отдельной таблице
    @OneToOne(mappedBy = "analysisResult", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AnalysisStatistics statistics;
}