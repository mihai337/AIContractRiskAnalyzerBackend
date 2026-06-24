package licenta.mihai.aicontractriskanalyzerbackend.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import licenta.mihai.aicontractriskanalyzerbackend.models.ContractAnalysisResult;

@Converter
public class ContractAnalysisResultConverter implements AttributeConverter<ContractAnalysisResult, String> {

    // Ignore unknown properties so analysis JSON written by an older schema (e.g. rows that
    // still contain the removed clauseInsight "evidence" field) keeps deserializing.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .findAndRegisterModules()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public String convertToDatabaseColumn(ContractAnalysisResult attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize contract analysis", ex);
        }
    }

    @Override
    public ContractAnalysisResult convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, ContractAnalysisResult.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to deserialize contract analysis", ex);
        }
    }
}

