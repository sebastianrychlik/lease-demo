package com.leasedemo.util;

import com.leasedemo.entity.Gender;
import com.leasedemo.exception.InvalidPeselException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PeselValidator}.
 *
 * <p>Test PESELs used below are well-known publicly-documented examples
 * used purely to exercise the checksum/date/gender algorithm; they do not
 * correspond to real people.
 */
@DisplayName("PeselValidator")
class PeselValidatorTest {

    private final PeselValidator validator = new PeselValidator();

    // 44051401359 → 1944-05-14, gender digit (index 9) = '5' -> odd -> male, valid checksum.
    private static final String MALE_PESEL = "44051401359";
    // 02270800027 → year 02, month 27 (century offset 20 -> 2000, month 07), day 08
    // -> 2002-07-08; gender digit (index 9) = '2' -> even -> female, valid checksum.
    private static final String FEMALE_PESEL_2000S = "02270800027";

    @Test
    @DisplayName("accepts a valid PESEL matching declared DOB and gender")
    void validate_validPesel_doesNotThrow() {
        LocalDate dob = validator.extractDateOfBirth(MALE_PESEL);
        Gender gender = validator.extractGender(MALE_PESEL);

        assertThat(gender).isEqualTo(Gender.MALE);
        validator.validate(MALE_PESEL, dob, gender);
    }

    @Test
    @DisplayName("extracts date of birth encoded with 2000s century offset")
    void extractDateOfBirth_2000sCentury_decodesCorrectly() {
        LocalDate dob = validator.extractDateOfBirth(FEMALE_PESEL_2000S);
        assertThat(dob).isEqualTo(LocalDate.of(2002, 7, 8));
        assertThat(validator.extractGender(FEMALE_PESEL_2000S)).isEqualTo(Gender.FEMALE);
    }

    @Test
    @DisplayName("rejects a PESEL that is not 11 digits")
    void validate_wrongLength_throws() {
        assertThatThrownBy(() -> validator.validate("123", LocalDate.now(), Gender.MALE))
                .isInstanceOf(InvalidPeselException.class);
    }

    @Test
    @DisplayName("rejects a PESEL containing non-digit characters")
    void validate_nonDigits_throws() {
        assertThatThrownBy(() -> validator.validate("4405140135A", LocalDate.now(), Gender.MALE))
                .isInstanceOf(InvalidPeselException.class);
    }

    @Test
    @DisplayName("rejects a PESEL with an invalid checksum")
    void validate_invalidChecksum_throws() {
        String tampered = "44051401350"; // last digit changed from correct 9
        assertThatThrownBy(() -> validator.validate(tampered, LocalDate.of(1944, 5, 14), Gender.MALE))
                .isInstanceOf(InvalidPeselException.class)
                .hasMessageContaining("checksum");
    }

    @Test
    @DisplayName("rejects a PESEL whose encoded DOB does not match the declared DOB")
    void validate_dobMismatch_throws() {
        assertThatThrownBy(() -> validator.validate(MALE_PESEL, LocalDate.of(1999, 1, 1), Gender.MALE))
                .isInstanceOf(InvalidPeselException.class)
                .hasMessageContaining("date of birth");
    }

    @Test
    @DisplayName("rejects a PESEL whose encoded gender does not match the declared gender")
    void validate_genderMismatch_throws() {
        LocalDate dob = validator.extractDateOfBirth(MALE_PESEL);
        assertThatThrownBy(() -> validator.validate(MALE_PESEL, dob, Gender.FEMALE))
                .isInstanceOf(InvalidPeselException.class)
                .hasMessageContaining("gender");
    }
}
