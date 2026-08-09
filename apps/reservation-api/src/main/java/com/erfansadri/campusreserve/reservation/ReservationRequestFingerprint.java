package com.erfansadri.campusreserve.reservation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

public final class ReservationRequestFingerprint {

    private ReservationRequestFingerprint() {
    }

    public static String create(
            Long eventId,
            String attendeeName,
            String attendeeEmail) {

        Objects.requireNonNull(eventId);
        Objects.requireNonNull(attendeeName);
        Objects.requireNonNull(attendeeEmail);

        String canonicalRequest = String.join(
                "\n",
                "eventId=" + eventId,
                "attendeeName=" + attendeeName.trim(),
                "attendeeEmail="
                        + attendeeEmail.trim().toLowerCase(Locale.ROOT));

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    canonicalRequest.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available.",
                    exception);
        }
    }
}