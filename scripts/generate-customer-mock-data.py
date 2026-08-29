#!/usr/bin/env python3
"""
generate-customer-mock-data.py
================================

LOCAL-DEVELOPMENT-ONLY mock Customer data generator for LeaseDemo (M4.1.3).

Generates a JSON array of synthetic Customer "seed records" that can be
consumed by the Spring Boot local seeding mechanism
(``CustomerSeedRunner``), which passes each record through the existing
``CustomerService`` business path (PESEL validation, AES-256-GCM
encryption, HMAC-SHA-256 lookup hashing, persistence).

This script deliberately does NOT:

- duplicate the Java AES/HMAC implementation
- write directly to PostgreSQL
- generate real personal data

It only produces plausible, structurally-valid synthetic business input
(including a structurally-valid Polish PESEL derived from the generated
date of birth and gender), for consumption by trusted, local-only Spring
Boot tooling.

Usage
-----

    python scripts/generate-customer-mock-data.py [--count N] [--output PATH] [--seed N]

    # Run the built-in self-tests instead of generating data:
    python scripts/generate-customer-mock-data.py --self-test

Standard library only — no third-party dependencies.
"""

from __future__ import annotations

import argparse
import json
import random
import sys
import unittest
from dataclasses import dataclass, asdict
from datetime import date, timedelta

# -----------------------------------------------------------------------
# Name data (small, built-in, non-exhaustive Polish name lists)
# -----------------------------------------------------------------------

MALE_FIRST_NAMES = [
    "Jan", "Piotr", "Tomasz", "Marek", "Adam", "Krzysztof", "Pawel", "Michal",
]

FEMALE_FIRST_NAMES = [
    "Anna", "Katarzyna", "Agnieszka", "Magdalena", "Joanna", "Monika", "Ewa",
    "Aleksandra",
]

# (male_form, female_form) surname pairs.
SURNAME_PAIRS = [
    ("Kowalski", "Kowalska"),
    ("Nowak", "Nowak"),
    ("Wisniewski", "Wisniewska"),
    ("Wojcik", "Wojcik"),
    ("Kaminski", "Kaminska"),
    ("Lewandowski", "Lewandowska"),
    ("Zielinski", "Zielinska"),
    ("Szymanski", "Szymanska"),
]

EMAIL_DOMAIN = "example.test"

# PESEL checksum weights, per the Polish PESEL specification.
CHECKSUM_WEIGHTS = (1, 3, 7, 9, 1, 3, 7, 9, 1, 3)

# Century -> month-offset encoding, per the Polish PESEL specification.
CENTURY_MONTH_OFFSETS = {
    (1800, 1899): 80,
    (1900, 1999): 0,
    (2000, 2099): 20,
    (2100, 2199): 40,
    (2200, 2299): 60,
}


# -----------------------------------------------------------------------
# PESEL generation
# -----------------------------------------------------------------------

def _month_offset_for_year(year: int) -> int:
    for (lo, hi), offset in CENTURY_MONTH_OFFSETS.items():
        if lo <= year <= hi:
            return offset
    raise ValueError(f"No known PESEL century encoding for year {year}")


def _checksum_digit(first_ten_digits: str) -> int:
    total = sum(int(d) * w for d, w in zip(first_ten_digits, CHECKSUM_WEIGHTS))
    return (10 - (total % 10)) % 10


def generate_pesel(date_of_birth: date, gender: str, serial_source: int) -> str:
    """
    Generates a structurally-valid Polish PESEL encoding ``date_of_birth``
    and ``gender``.

    :param date_of_birth: the date of birth to encode.
    :param gender: "MALE" or "FEMALE".
    :param serial_source: an integer used to derive the 3-digit ordinal
        serial segment (digits 7-9) and disambiguate the gender digit;
        the caller is responsible for probing further values on collision
        to guarantee batch uniqueness.
    """
    if gender not in ("MALE", "FEMALE"):
        raise ValueError(f"Unsupported gender: {gender}")

    year = date_of_birth.year
    month_offset = _month_offset_for_year(year)
    encoded_month = date_of_birth.month + month_offset

    yy = year % 100
    digits = f"{yy:02d}{encoded_month:02d}{date_of_birth.day:02d}"

    # Digits 7-9: 3-digit ordinal serial number, made unique per record by
    # the caller via `serial_source`.
    serial = serial_source % 1000
    digits += f"{serial:03d}"

    # Digit 10: gender digit — odd => MALE, even => FEMALE. Derived from
    # serial_source, then nudged to match the required parity.
    gender_digit = serial_source % 10
    if gender == "MALE" and gender_digit % 2 == 0:
        gender_digit = (gender_digit + 1) % 10
    elif gender == "FEMALE" and gender_digit % 2 == 1:
        gender_digit = (gender_digit + 1) % 10
    digits += str(gender_digit)

    assert len(digits) == 10, digits
    check_digit = _checksum_digit(digits)
    pesel = digits + str(check_digit)
    assert len(pesel) == 11
    return pesel


def extract_date_of_birth(pesel: str) -> date:
    """Mirrors PeselValidator.extractDateOfBirth for self-testing."""
    yy = int(pesel[0:2])
    encoded_month = int(pesel[2:4])
    day = int(pesel[4:6])

    month_offset = (encoded_month // 20) * 20
    century_by_offset = {0: 1900, 20: 2000, 40: 2100, 60: 2200, 80: 1800}
    if month_offset not in century_by_offset:
        raise ValueError(f"Invalid PESEL month segment: {encoded_month}")
    century = century_by_offset[month_offset]
    month = encoded_month - month_offset
    return date(century + yy, month, day)


def extract_gender(pesel: str) -> str:
    """Mirrors PeselValidator.extractGender for self-testing."""
    gender_digit = int(pesel[9])
    return "FEMALE" if gender_digit % 2 == 0 else "MALE"


def is_checksum_valid(pesel: str) -> bool:
    return _checksum_digit(pesel[:10]) == int(pesel[10])


# -----------------------------------------------------------------------
# Customer record generation
# -----------------------------------------------------------------------

@dataclass(frozen=True)
class CustomerSeedRecord:
    keycloakUserId: str
    firstName: str
    lastName: str
    email: str
    phoneNumber: str
    dateOfBirth: str
    gender: str
    pesel: str


def _random_date_of_birth(rng: random.Random) -> date:
    start = date(1945, 1, 1)
    end = date(2005, 12, 31)
    delta_days = (end - start).days
    return start + timedelta(days=rng.randint(0, delta_days))


def _strip_polish_diacritics(text: str) -> str:
    table = str.maketrans("acelnoszzACELNOSZZ", "acelnoszzACELNOSZZ")
    return text.translate(table)


def generate_customers(count: int, seed: int | None = None) -> list[dict]:
    if count <= 0:
        raise ValueError("count must be a positive integer")

    rng = random.Random(seed)
    used_pesels: set[str] = set()
    records: list[dict] = []

    for i in range(1, count + 1):
        gender = "MALE" if rng.random() < 0.5 else "FEMALE"
        first_name = rng.choice(MALE_FIRST_NAMES if gender == "MALE" else FEMALE_FIRST_NAMES)
        male_surname, female_surname = rng.choice(SURNAME_PAIRS)
        last_name = male_surname if gender == "MALE" else female_surname

        dob = _random_date_of_birth(rng)

        # Ensure PESEL uniqueness within the batch: vary the serial source
        # deterministically per index, and probe forward on collision.
        serial_source = i
        pesel = generate_pesel(dob, gender, serial_source)
        probe = 0
        while pesel in used_pesels:
            probe += 1
            serial_source = i + probe * count
            pesel = generate_pesel(dob, gender, serial_source)
        used_pesels.add(pesel)

        keycloak_user_id = f"mock-user-{i:06d}"
        email_local = f"{_strip_polish_diacritics(first_name)}.{_strip_polish_diacritics(last_name)}.{i:06d}".lower()
        email = f"{email_local}@{EMAIL_DOMAIN}"
        phone_number = f"+48{100000000 + i:09d}"[:12]

        record = CustomerSeedRecord(
            keycloakUserId=keycloak_user_id,
            firstName=first_name,
            lastName=last_name,
            email=email,
            phoneNumber=phone_number,
            dateOfBirth=dob.isoformat(),
            gender=gender,
            pesel=pesel,
        )
        records.append(asdict(record))

    return records


# -----------------------------------------------------------------------
# Self-validation (fails fast: a generator bug must not silently pass
# invalid PESELs downstream — Spring's PeselValidator remains the
# authoritative application validation, but the generator must never rely
# on that alone).
# -----------------------------------------------------------------------

def self_validate_batch(records: list[dict]) -> None:
    seen_pesels: set[str] = set()
    seen_ids: set[str] = set()
    seen_emails: set[str] = set()

    for record in records:
        pesel = record["pesel"]
        dob = date.fromisoformat(record["dateOfBirth"])
        gender = record["gender"]

        if len(pesel) != 11 or not pesel.isdigit():
            raise AssertionError(f"Generated PESEL is not exactly 11 digits: len={len(pesel)}")
        if extract_date_of_birth(pesel) != dob:
            raise AssertionError("Generated PESEL does not encode the declared date of birth")
        if extract_gender(pesel) != gender:
            raise AssertionError("Generated PESEL does not encode the declared gender")
        if not is_checksum_valid(pesel):
            raise AssertionError("Generated PESEL checksum is invalid")

        if pesel in seen_pesels:
            raise AssertionError("Duplicate PESEL generated within batch")
        seen_pesels.add(pesel)

        if record["keycloakUserId"] in seen_ids:
            raise AssertionError("Duplicate keycloakUserId generated within batch")
        seen_ids.add(record["keycloakUserId"])

        if record["email"] in seen_emails:
            raise AssertionError("Duplicate email generated within batch")
        seen_emails.add(record["email"])


# -----------------------------------------------------------------------
# CLI
# -----------------------------------------------------------------------

def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Generate LOCAL-DEVELOPMENT-ONLY mock Customer data for LeaseDemo.")
    parser.add_argument("--count", type=int, default=50, help="number of customers to generate (default: 50)")
    parser.add_argument("--output", type=str, default=None,
                         help="output JSON file path (default: stdout)")
    parser.add_argument("--seed", type=int, default=None, help="optional RNG seed for reproducibility")
    parser.add_argument("--self-test", action="store_true",
                         help="run the built-in PESEL self-tests instead of generating data")
    args = parser.parse_args(argv)

    if args.self_test:
        suite = unittest.TestLoader().loadTestsFromModule(sys.modules[__name__])
        result = unittest.TextTestRunner(verbosity=2).run(suite)
        return 0 if result.wasSuccessful() else 1

    records = generate_customers(args.count, seed=args.seed)
    self_validate_batch(records)

    output_json = json.dumps(records, indent=2, ensure_ascii=False)

    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            f.write(output_json)
        # Never print PESEL values or full payloads to the console — only a
        # non-sensitive summary.
        print(f"Generated {len(records)} mock customers -> {args.output}", file=sys.stderr)
    else:
        print(output_json)

    return 0


# -----------------------------------------------------------------------
# Tests (standard-library unittest; also runnable via --self-test)
# -----------------------------------------------------------------------

class PeselGenerationTests(unittest.TestCase):

    def test_male_pesel_has_odd_gender_digit(self):
        dob = date(1990, 5, 14)
        pesel = generate_pesel(dob, "MALE", serial_source=1)
        self.assertEqual(int(pesel[9]) % 2, 1)
        self.assertEqual(extract_gender(pesel), "MALE")

    def test_female_pesel_has_even_gender_digit(self):
        dob = date(1990, 5, 14)
        pesel = generate_pesel(dob, "FEMALE", serial_source=2)
        self.assertEqual(int(pesel[9]) % 2, 0)
        self.assertEqual(extract_gender(pesel), "FEMALE")

    def test_checksum_is_correct(self):
        dob = date(1985, 1, 1)
        pesel = generate_pesel(dob, "MALE", serial_source=42)
        self.assertTrue(is_checksum_valid(pesel))

    def test_dob_encoding_round_trips(self):
        dob = date(1978, 11, 23)
        pesel = generate_pesel(dob, "FEMALE", serial_source=7)
        self.assertEqual(extract_date_of_birth(pesel), dob)

    def test_1900_century_encoding(self):
        dob = date(1999, 12, 31)
        pesel = generate_pesel(dob, "MALE", serial_source=3)
        self.assertEqual(int(pesel[2:4]), 12)
        self.assertEqual(extract_date_of_birth(pesel), dob)

    def test_2000_century_encoding(self):
        dob = date(2001, 6, 15)
        pesel = generate_pesel(dob, "FEMALE", serial_source=9)
        self.assertEqual(int(pesel[2:4]), 26)
        self.assertEqual(extract_date_of_birth(pesel), dob)

    def test_1800_century_encoding(self):
        dob = date(1888, 3, 4)
        pesel = generate_pesel(dob, "MALE", serial_source=11)
        self.assertEqual(int(pesel[2:4]), 83)
        self.assertEqual(extract_date_of_birth(pesel), dob)

    def test_2100_century_encoding(self):
        dob = date(2105, 7, 19)
        pesel = generate_pesel(dob, "FEMALE", serial_source=13)
        self.assertEqual(int(pesel[2:4]), 47)
        self.assertEqual(extract_date_of_birth(pesel), dob)

    def test_2200_century_encoding(self):
        dob = date(2210, 9, 9)
        pesel = generate_pesel(dob, "MALE", serial_source=17)
        self.assertEqual(int(pesel[2:4]), 69)
        self.assertEqual(extract_date_of_birth(pesel), dob)

    def test_batch_uniqueness(self):
        records = generate_customers(200, seed=1234)
        self_validate_batch(records)
        pesels = [r["pesel"] for r in records]
        ids = [r["keycloakUserId"] for r in records]
        emails = [r["email"] for r in records]
        self.assertEqual(len(pesels), len(set(pesels)))
        self.assertEqual(len(ids), len(set(ids)))
        self.assertEqual(len(emails), len(set(emails)))

    def test_default_batch_is_internally_consistent(self):
        records = generate_customers(50, seed=42)
        self_validate_batch(records)
        self.assertEqual(len(records), 50)


if __name__ == "__main__":
    raise SystemExit(main())
