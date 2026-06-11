package mx.edu.cenidet.estadias.modelos.token;

import jakarta.persistence.AttributeConverter;

public class SiNoBooleanConventer implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean attribute) {
        return Boolean.TRUE.equals(attribute) ? "SI" : "NO";
    }

    @Override
    public Boolean convertToEntityAttribute(String dbData) {
        return "SI".equalsIgnoreCase(dbData);
    }

}
