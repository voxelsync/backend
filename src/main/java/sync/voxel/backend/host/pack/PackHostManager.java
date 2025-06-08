package sync.voxel.backend.host.pack;

import sync.voxel.backend.VoxelApp;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PackHostManager {
    private static final String ADMIN_AUTH = System.getenv().getOrDefault("ADMIN_AUTH", "demo-admin-key");
    private final Map<String, PackProfile> activeProfiles = new HashMap<>();
    private final Set<String> availableKeys = new HashSet<>();

    public boolean isAdminAuthorized(String adminAuth) {
        if(ADMIN_AUTH == null) return false;
        return ADMIN_AUTH.equals(adminAuth);
    }

    public void registerHostKey(String hostKey) {
        if (activeProfiles.containsKey(hostKey)) {
            VoxelApp.LOGGER.debug("Host key already active: {}", hostKey);
            return;
        }

        if (availableKeys.add(hostKey)) {
            VoxelApp.LOGGER.info("Registered new host key: {}", hostKey);
            return;
        }
        VoxelApp.LOGGER.debug("Host key already available: {}", hostKey);
    }

    public boolean isKeyMatchToIdentifier(String hostKey, String identifier) {
        PackProfile profile = activeProfiles.get(hostKey);
        return profile != null && profile.matchesIdentifier(identifier);
    }

    public void rotateIdentifierForHostKey(String hostKey, String oldIdentifier, String newIdentifier) {
        PackProfile profile = activeProfiles.get(hostKey);
        if (profile == null) {
            VoxelApp.LOGGER.debug("Host key not found for rotation: {}", hostKey);
            return;
        }

        if (profile.rotateIdentifier(oldIdentifier, newIdentifier)) {
            VoxelApp.LOGGER.info("Identifier rotated for {}: {} -> {}", hostKey, oldIdentifier, newIdentifier);
            return;
        }
        VoxelApp.LOGGER.warn("Identifier rotation failed for {}", hostKey);
    }

    public String uploadPacketToGitHub(String hostKey, String identifier, String base64Pack) {
        if (!activeProfiles.containsKey(hostKey)) {
            activeProfiles.put(hostKey, new PackProfile(hostKey, identifier));
            VoxelApp.LOGGER.info("Created new profile for {} -> {}", hostKey, identifier);
        }

        PackProfile profile = activeProfiles.get(hostKey);
        if (!profile.matchesIdentifier(identifier)) {
            VoxelApp.LOGGER.warn("Identifier mismatch for {} (expected: {}, got: {})",
                    hostKey, profile.getCurrentIdentifier(), identifier);
            return null;
        }

        VoxelApp.LOGGER.info("Processing pack upload for {} [{}]", hostKey, identifier);
        String url = GitHubExecutor.uploadToGithub(identifier, base64Pack);
        VoxelApp.LOGGER.info("Upload completed for {}: {}", identifier, url);
        return url;
    }

    public void removeHostKey(String hostKey) {
        PackProfile profile = activeProfiles.get(hostKey);
        activeProfiles.remove(hostKey);
        availableKeys.add(hostKey);
        try {
            GitHubExecutor.deleteExistingReleaseIfExists(GitHubExecutor.createTagName(profile.getCurrentIdentifier()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        VoxelApp.LOGGER.info("Remove host key {} [{}]", hostKey, profile.getCurrentIdentifier());
    }
}