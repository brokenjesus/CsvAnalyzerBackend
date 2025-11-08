package by.lupach.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AnalysisStatisticsRepository extends JpaRepository<by.lupach.backend.entities.AnalysisStatistics, UUID> {
}