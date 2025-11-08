package by.lupach.backend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Table(name = "analysis_statistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisStatistics {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_result_id")
    private AnalysisResult analysisResult;

    private Long totalRecords;
    private Long processedRecords;
    private Long skippedRecords;

    private Double minValue;
    private Double maxValue;
    private Double meanValue;
    private Double stdDeviation;
    private Long uniqueValuesCount;

    private Double sumValue;
    private Double sumOfSquares;


}
