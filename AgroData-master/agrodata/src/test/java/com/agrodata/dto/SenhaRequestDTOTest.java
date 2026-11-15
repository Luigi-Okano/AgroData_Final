package com.agrodata.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SenhaRequestDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsNewPasswordsFromEightThroughSeventyTwoCharacters() {
        assertTrue(validator.validate(dtoWithNewPassword("12345678")).isEmpty());
        assertTrue(validator.validate(dtoWithNewPassword("a".repeat(72))).isEmpty());
    }

    @Test
    void rejectsNewPasswordsOutsideEightToTenCharacters() {
        assertFalse(validator.validate(dtoWithNewPassword("1234567")).isEmpty());
        assertFalse(validator.validate(dtoWithNewPassword("a".repeat(73))).isEmpty());
    }

    private SenhaRequestDTO dtoWithNewPassword(String password) {
        SenhaRequestDTO dto = new SenhaRequestDTO();
        dto.setSenhaAtual("senha-atual");
        dto.setNovaSenha(password);
        return dto;
    }
}
