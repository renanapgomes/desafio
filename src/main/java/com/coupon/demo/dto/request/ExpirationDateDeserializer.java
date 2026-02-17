package com.coupon.demo.dto.request;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Aceita data em "yyyy-MM-dd" ou "dd-MM-yyyy" e normaliza para "yyyy-MM-dd".
 */
public class ExpirationDateDeserializer extends JsonDeserializer<String> {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String value = p.getText();
        if (value == null || value.isBlank()) {
            return value;
        }
        value = value.trim();
        try {
            LocalDate date = LocalDate.parse(value, DD_MM_YYYY);
            return date.format(ISO);
        } catch (DateTimeParseException e1) {
            try {
                LocalDate date = LocalDate.parse(value, ISO);
                return date.format(ISO);
            } catch (DateTimeParseException e2) {
                throw new JsonProcessingException(
                        "Data de expiração inválida. Use yyyy-MM-dd ou dd-MM-yyyy (ex.: 2026-12-31 ou 31-12-2026).") {};
            }
        }
    }
}
