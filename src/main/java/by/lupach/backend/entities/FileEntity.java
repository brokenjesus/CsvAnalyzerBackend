package by.lupach.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    private String fileName;
    private Long fileSize;
    private String filePath;
    private LocalDateTime uploadTime;

    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    @OneToOne(mappedBy = "file", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AnalysisResult analysisResult;

    @PrePersist
    public void prePersist() {
        if (id != null && fileName != null) {
            this.filePath = id + "_" + fileName;
//            this.filePath = "uploads/" + id + "_" + fileName;
        }
    }
}
