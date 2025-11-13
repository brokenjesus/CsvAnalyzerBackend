package by.lupach.backend.configs;

import by.lupach.backend.converters.AnalysisResultToDtoConverter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.ConfigurableConversionService;


@Configuration
@RequiredArgsConstructor
public class ConversionConfig {

    private final ConfigurableConversionService conversionService;
    private final AnalysisResultToDtoConverter analysisResultToDtoConverter;

    @PostConstruct
    public void registerConverters() {
        conversionService.addConverter(analysisResultToDtoConverter);
    }
}