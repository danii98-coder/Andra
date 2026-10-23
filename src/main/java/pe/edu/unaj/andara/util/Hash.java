package pe.edu.unaj.andara.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Hash {

    private Hash() {
    }

    public static long numero(String texto) {
        long h = 0;
        String s = String.valueOf(texto);
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            char primero = s.charAt(i);
            h = (h * 31 + primero) & 0xFFFFFFFFL;
            i += Character.charCount(cp);
        }
        return h;
    }

    public static byte[] sha256(String texto) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String sha256Hex(String texto) {
        return HexFormat.of().formatHex(sha256(texto));
    }
}
