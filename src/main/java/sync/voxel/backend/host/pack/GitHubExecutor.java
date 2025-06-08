package sync.voxel.backend.host.pack;

import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import sync.voxel.backend.VoxelApp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

public class GitHubExecutor {

    private static final String REPO = "";
    public static String uploadToGithub(String mac, String base64Pack) {
        if (GITHUB_TOKEN == null || GITHUB_TOKEN.isEmpty()) {
            VoxelApp.LOGGER.error("GitHub token is not configured!");
            return "GitHub token not configured";
        }

        VoxelApp.LOGGER.info("Starting GitHub upload for MAC: {}", mac);

        try {
            GitHub github = connectToGitHub();
            GHRepository repo = getRepository(github);
            byte[] data = decodeBase64Pack(base64Pack);

            String tag = createTagName(mac);
            deleteExistingReleaseIfExists(repo, tag);

            GHRelease release = createNewRelease(repo, tag, mac);
            uploadAssetToRelease(release, mac, data);

            return findAssetDownloadUrl(release, mac);

        } catch (Exception e) {
            VoxelApp.LOGGER.error("GitHub upload failed for MAC: {}", mac, e);
            return "Upload failed: " + e.getMessage();
        }
    }

    private static GitHub connectToGitHub() throws IOException {
        VoxelApp.LOGGER.debug("Connecting to GitHub...");
        GitHub github = new GitHubBuilder().withOAuthToken(GITHUB_TOKEN).build();
        VoxelApp.LOGGER.debug("Successfully connected to GitHub");
        return github;
    }

    private static GHRepository getRepository(GitHub github) throws IOException {
        VoxelApp.LOGGER.debug("Accessing repository: {}", REPO);
        GHRepository repo = github.getRepository(REPO);
        VoxelApp.LOGGER.debug("Successfully accessed repository");
        return repo;
    }

    private static byte[] decodeBase64Pack(String base64Pack) {
        VoxelApp.LOGGER.debug("Decoding base64 pack data");
        byte[] data = Base64.getDecoder().decode(base64Pack);
        VoxelApp.LOGGER.debug("Decoded pack data (size: {} bytes)", data.length);
        return data;
    }

    private static String createTagName(String mac) {
        return "pack-" + mac.replace(":", "-");
    }

    private static void deleteExistingReleaseIfExists(GHRepository repo, String tag) throws IOException {
        try {
            GHRelease old = repo.getReleaseByTagName(tag);
            if (old != null) {
                VoxelApp.LOGGER.info("Found existing release for tag: {}, deleting...", tag);
                old.delete();
                VoxelApp.LOGGER.info("Deleted old release: {}", tag);
            }
        } catch (Exception e) {
            VoxelApp.LOGGER.debug("No existing release found for tag: {}", tag);
        }
    }

    private static GHRelease createNewRelease(GHRepository repo, String tag, String mac) throws IOException {
        VoxelApp.LOGGER.debug("Creating new release: {}", tag);
        String releaseName = "Pack for " + mac;
        GHRelease release = repo.createRelease(tag)
                .name(releaseName)
                .prerelease(false)
                .create();
        VoxelApp.LOGGER.info("Created new release: {}", release.getHtmlUrl());
        return release;
    }

    private static void uploadAssetToRelease(GHRelease release, String mac, byte[] data) throws IOException {
        VoxelApp.LOGGER.debug("Uploading asset...");
        release.uploadAsset(
                mac + ".zip",
                new ByteArrayInputStream(data),
                "application/zip"
        );
        VoxelApp.LOGGER.info("Asset uploaded successfully");
    }

    private static String findAssetDownloadUrl(GHRelease release, String mac) throws IOException {
        for (GHAsset asset : release.listAssets()) {
            if (asset.getName().equals(mac.replace(":", ".") + ".zip")) {
                String url = asset.getBrowserDownloadUrl();
                VoxelApp.LOGGER.info("Asset download URL: {}", url);
                return url;
            }
        }
        VoxelApp.LOGGER.error("Uploaded asset not found in release assets");
        return "Asset upload failed or not found.";
    }
}