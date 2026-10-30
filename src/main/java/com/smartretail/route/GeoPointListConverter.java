package com.smartretail.route;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Stores polygon points as JSON: [{"lat":17.4,"lng":78.4}, ...]. */
@Converter
class GeoPointListConverter implements AttributeConverter<List<GeoPoint>, String> {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<List<GeoPoint>> POINTS = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<GeoPoint> points) {
        return points == null ? null : JSON.writeValueAsString(points);
    }

    @Override
    public List<GeoPoint> convertToEntityAttribute(String json) {
        return json == null || json.isBlank() ? null : JSON.readValue(json, POINTS);
    }
}
