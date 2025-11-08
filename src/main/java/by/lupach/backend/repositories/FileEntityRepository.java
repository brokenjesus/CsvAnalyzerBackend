package by.lupach.backend.repositories;

import by.lupach.backend.entities.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileEntityRepository extends JpaRepository<FileEntity, UUID> {

    List<FileEntity> findByStatusOrderByUploadTimeDesc(by.lupach.backend.entities.ProcessingStatus status);
}