package sync.voxel.backend.host.pack;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter
public class PackProfile {
    private final String hostKey;
    private final String encryptionKey;
    private String currentIdentifier;

    public PackProfile(@NotNull String hostKey, @NotNull String initialIdentifier) {
        this.hostKey = hostKey;
        this.currentIdentifier = initialIdentifier;
        this.encryptionKey = generateEncryptionKey();
    }

    @NotNull
    private String generateEncryptionKey() {
        String allChars = "abcdefghijklmnopqrstuvwxyz.-_";
        List<Character> chars = allChars.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
        Collections.shuffle(chars);
        return chars.stream()
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    public boolean matchesIdentifier(@NotNull String identifier) {
        return currentIdentifier.equals(identifier);
    }

    public boolean rotateIdentifier(@NotNull String oldIdentifier, @NotNull String newIdentifier) {
        if (currentIdentifier.equals(oldIdentifier)) {
            currentIdentifier = newIdentifier;
            return true;
        }
        return false;
    }

    @NotNull
    public static String encrypt(@NotNull String input, @NotNull String key) {
        if (key.length() < 26) throw new IllegalArgumentException("Encryption key must contain at least 26 characters");

        return input.chars()
                .mapToObj(c -> {
                    char lower = Character.toLowerCase((char) c);
                    if (lower >= 'a' && lower <= 'z') {
                        return String.valueOf(Character.toUpperCase(key.charAt(lower - 'a')));
                    } else if (".-_".indexOf(c) != -1) {
                        return String.valueOf(Character.toUpperCase((char) c));
                    }
                    return ".";
                })
                .collect(Collectors.joining());
    }

    @NotNull
    public String getSaveIdentifier() {
        return encrypt(currentIdentifier, encryptionKey);
    }
}