package com.leasedemo.util;

import com.leasedemo.entity.Gender;
import com.leasedemo.exception.InvalidPeselException;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;

/**
 * Validates Polish PESEL national identification numbers.
 *
 * <p>A PESEL is an 11-digit number encoding: date of birth (digits 1-6),
 * a serial/gender segment (digits 7-10) and a checksum digit (digit 11).
 * This validator:
 *
 * <ul>
 *   <li>enforces the 11-digit numeric format</li>
 *   <li>verifies the checksum digit</li>
 *   <li>extracts the encoded date of birth and cross-checks it against the
 *       declared {@link LocalDate} of birth</li>
 *   <li>extracts the encoded gender and cross-checks it against the
 *       declared {@link Gender}</li>
 * </ul>
 *
 * <p>Any mismatch results in an {@link InvalidPeselException} — the PESEL is
 * never persisted (encrypted or otherwise) unless it passes every check.
 */
@Component
public class PeselValidator {

    private static final int PESEL_LENGTH = 11;

    /** Checksum weights applied to the first 10 digits, per the PESEL spec. */
    private static final int[] CHECKSUM_WEIGHTS = {1, 3, 7, 9, 1, 3, 7, 9, 1, 3};

    /**
     * Validates {@code pesel} structurally and cross-checks it against the
     * declared date of birth and gender.
     *
     * @throws InvalidPeselException if the PESEL is malformed, fails its
     *                                checksum, or does not match the
     *                                declared date of birth / gender.
     */
    public void validate(String pesel, LocalDate declaredDateOfBirth, Gender declaredGender) {
        requireWellFormed(pesel);
        requireValidChecksum(pesel);

        LocalDate encodedDateOfBirth = extractDateOfBirth(pesel);
        if (!encodedDateOfBirth.equals(declaredDateOfBirth)) {
            throw new InvalidPeselException(
                    "PESEL date of birth does not match the declared date of birth");
        }

        Gender encodedGender = extractGender(pesel);
        if (encodedGender != declaredGender) {
            throw new InvalidPeselException("PESEL gender does not match the declared gender");
        }
    }

    private void requireWellFormed(String pesel) {
        if (pesel == null || pesel.length() != PESEL_LENGTH || !pesel.chars().allMatch(Character::isDigit)) {
            throw new InvalidPeselException("PESEL must be exactly 11 digits");
        }
    }

    private void requireValidChecksum(String pesel) {
        int sum = 0;
        for (int i = 0; i < CHECKSUM_WEIGHTS.length; i++) {
            sum += CHECKSUM_WEIGHTS[i] * digitAt(pesel, i);
        }
        int checksum = sum % 10;
        int expectedControlDigit = (10 - checksum) % 10;
        int actualControlDigit = digitAt(pesel, 10);

        if (expectedControlDigit != actualControlDigit) {
            throw new InvalidPeselException("PESEL checksum is invalid");
        }
    }

    /**
     * Extracts the date of birth encoded in digits 1-6, decoding the century
     * from the offset added to the month (see PESEL specification).
     */
    public LocalDate extractDateOfBirth(String pesel) {
        requireWellFormed(pesel);

        int year = digitsAt(pesel, 0, 2);
        int month = digitsAt(pesel, 2, 4);
        int day = digitsAt(pesel, 4, 6);

        int century;
        int monthOffset = (month / 20) * 20;
        switch (monthOffset) {
            case 0 -> century = 1900;
            case 20 -> century = 2000;
            case 40 -> century = 2100;
            case 60 -> century = 2200;
            case 80 -> century = 1800;
            default -> throw new InvalidPeselException("PESEL month segment is invalid");
        }
        month -= monthOffset;

        try {
            return LocalDate.of(century + year, month, day);
        } catch (DateTimeException ex) {
            throw new InvalidPeselException("PESEL does not encode a valid calendar date");
        }
    }

    /**
     * Extracts the gender encoded in digit 10 (1-based): odd = male, even =
     * female.
     */
    public Gender extractGender(String pesel) {
        requireWellFormed(pesel);
        int genderDigit = digitAt(pesel, 9);
        return (genderDigit % 2 == 0) ? Gender.FEMALE : Gender.MALE;
    }

    private int digitAt(String pesel, int index) {
        return Character.digit(pesel.charAt(index), 10);
    }

    private int digitsAt(String pesel, int fromInclusive, int toExclusive) {
        return Integer.parseInt(pesel.substring(fromInclusive, toExclusive));
    }
}
