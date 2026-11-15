package com.agrodata.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FazendaRequestDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void allowsCoordinatesToBeOmittedTogether() {
        assertTrue(validator.validateProperty(new FazendaRequestDTO(), "coordinatePairValid").isEmpty());
    }

    @Test
    void requiresLatitudeAndLongitudeToBeProvidedTogether() {
        FazendaRequestDTO dto = new FazendaRequestDTO();
        dto.setLatitude(-23.5);

        assertFalse(validator.validateProperty(dto, "coordinatePairValid").isEmpty());
    }

    @Test
    void rejectsCoordinatesOutsideEarthBounds() {
        FazendaRequestDTO dto = new FazendaRequestDTO();
        dto.setLatitude(91.0);
        dto.setLongitude(-46.0);

        assertFalse(validator.validateProperty(dto, "latitude").isEmpty());
    }
}
