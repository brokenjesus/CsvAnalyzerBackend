package by.lupach.backend.services.fileprocessing;

import by.lupach.backend.entities.AnalysisStatistics;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class StatisticsCalculator {

    public void processLine(String line, AnalysisStatistics stats, Set<Double> unique) {
        stats.setTotalRecords(stats.getTotalRecords() + 1);
        try {
            String[] parts = line.split(",", 2);
            if (parts.length != 2) {
                stats.setSkippedRecords(stats.getSkippedRecords() + 1);
                return;
            }

            double value = Double.parseDouble(parts[1].trim());
            stats.setProcessedRecords(stats.getProcessedRecords() + 1);
            stats.setMinValue(Math.min(stats.getMinValue(), value));
            stats.setMaxValue(Math.max(stats.getMaxValue(), value));
            stats.setSumValue(stats.getSumValue() + value);
            stats.setSumOfSquares(stats.getSumOfSquares() + (value * value));
            unique.add(value);
        } catch (Exception e) {
            stats.setSkippedRecords(stats.getSkippedRecords() + 1);
        }
    }

    public void finalizeStats(AnalysisStatistics stats, Set<Double> unique) {
        long processed = stats.getProcessedRecords();
        if (processed > 0) {
            double mean = stats.getSumValue() / processed;
            stats.setMeanValue(mean);
            double variance = (stats.getSumOfSquares() / processed) - mean * mean;
            stats.setStdDeviation(Math.sqrt(Math.max(variance, 0)));
        }
        if (stats.getMinValue() == Double.MAX_VALUE) stats.setMinValue(0d);
        if (stats.getMaxValue() == Double.MIN_VALUE) stats.setMaxValue(0d);
        stats.setUniqueValuesCount((long) unique.size());
    }

    public Set<Double> newUniqueSet() {
        return new HashSet<>();
    }
}
