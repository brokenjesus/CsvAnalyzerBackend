package by.lupach.backend.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "analysisResult")
@EqualsAndHashCode(exclude = "analysisResult")
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

    @OneToOne(
            mappedBy = "file",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private AnalysisResult analysisResult;

    @PrePersist
    public void prePersist() {
        if (id != null && fileName != null) {
            this.filePath = id + "_" + fileName;
        }
    }
}
