package com.erfansadri.campusreserve.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReservationRequestFingerprintTests {

    @Test
    void producesSixtyFourCharacterSha256Fingerprint() {
        String fingerprint =
                ReservationRequestFingerprint.create(
                        78L,
                        "Test Student",
                        "student@example.com");

        assertThat(fingerprint)
                .hasSize(64)
                .matches("[0-9a-f]{64}");
    }

    @Test
    void producesSameFingerprintForNormalizedEmail() {
        String first =
                ReservationRequestFingerprint.create(
                        78L,
                        "Test Student",
                        "Student@Example.com");

        String second =
                ReservationRequestFingerprint.create(
                        78L,
                        "Test Student",
                        "  student@example.com  ");

        assertThat(first).isEqualTo(second);
    }

    @Test
    void producesDifferentFingerprintWhenRequestChanges() {
        String first =
                ReservationRequestFingerprint.create(
                        78L,
                        "Test Student",
                        "student@example.com");

        String second =
                ReservationRequestFingerprint.create(
                        78L,
                        "Different Student",
                        "different@example.com");

        assertThat(first).isNotEqualTo(second);
    }
}