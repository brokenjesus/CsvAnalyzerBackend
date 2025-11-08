package by.lupach.backend.repositories;

import by.lupach.backend.entities.AnalysisResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, UUID> {

    @Query("SELECT ar FROM AnalysisResult ar ORDER BY ar.processStartTime DESC")
    Page<AnalysisResult> findLatestAnalysisResults(Pageable pageable);

    @Query("SELECT COUNT(ar) FROM AnalysisResult ar")
    long countAnalysisResults();

    @Query("SELECT ar FROM AnalysisResult ar ORDER BY ar.processStartTime ASC")
    List<AnalysisResult> findOldestResults(Pageable pageable);

}