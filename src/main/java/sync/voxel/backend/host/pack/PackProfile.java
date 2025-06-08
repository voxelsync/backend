// PackProfile.java
package sync.voxel.backend.host.pack;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

@Getter @Setter
public class PackProfile {
    private final String hostKey;
    private final String encryptionKey;
    private String currentIdentifier;

    public PackProfile(String hostKey, String initialIdentifier) {
        this.hostKey = hostKey;
        this.currentIdentifier = initialIdentifier;
        this.encryptionKey = generateEncryptionKey();
    }

    @NotNull
    private String generateEncryptionKey() {
        List<Character> chars = new ArrayList<>(IntStream.rangeClosed('a','z').mapToObj(c->(char)c).toList());
        Collections.shuffle(chars);
        return chars.stream().collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
    }

    public boolean matchesIdentifier(String identifier) {
        return currentIdentifier.equals(identifier);
    }

    public boolean rotateIdentifier(String oldIdentifier, String newIdentifier) {
        if (currentIdentifier.equals(oldIdentifier)) {
            currentIdentifier = newIdentifier;
            return true;
        }
        return false;
    }
}