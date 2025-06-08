package sync.voxel.backend.host.pack;

import io.github.cdimascio.dotenv.Dotenv;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.github.*;
import sync.voxel.backend.VoxelApp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

public class GitHubExecutor {

    private static final String REPO_NAME = "voxelsync/host";
    private static String GITHUB_TOKEN = "";
    private static GHRepository repo;

    public static String uploadToGithub(String identifier, String base64Pack) {

        Dotenv dotenv = Dotenv.load();

        GITHUB_TOKEN = dotenv.get("GITHUB_TOKEN");

        if (GITHUB_TOKEN == null || GITHUB_TOKEN.isEmpty()) {
            VoxelApp.LOGGER.error("GitHub token is not configured!");
            return "GitHub token not configured";
        }

        VoxelApp.LOGGER.info("Starting GitHub upload for identifier: {}", identifier);

        try {
            GitHub github = connectToGitHub();
            getRepository(github);
            byte[] data = decodeBase64Pack(base64Pack);

            String tag = createTagName(identifier);
            deleteExistingReleaseIfExists(tag);

            GHRelease release = createNewRelease(tag, identifier);
            uploadAssetToRelease(release, identifier, data);

            return findAssetDownloadUrl(release, identifier);

        } catch (Exception e) {
            VoxelApp.LOGGER.error("GitHub upload failed for identifier: {}", identifier, e);
            return "Upload failed: " + e.getMessage();
        }
    }

    private static GitHub connectToGitHub() throws IOException {
        VoxelApp.LOGGER.debug("Connecting to GitHub...");
        GitHub github = new GitHubBuilder().withOAuthToken(GITHUB_TOKEN).build();
        VoxelApp.LOGGER.debug("Successfully connected to GitHub");
        return github;
    }

    private static void getRepository(@NotNull GitHub github) throws IOException {
        VoxelApp.LOGGER.debug("Accessing repository: {}", REPO_NAME);
        repo = github.getRepository(REPO_NAME);
        VoxelApp.LOGGER.debug("Successfully accessed repository");
    }

    private static byte @NotNull [] decodeBase64Pack(String base64Pack) {
        VoxelApp.LOGGER.debug("Decoding base64 pack data");
        byte[] data = Base64.getDecoder().decode(base64Pack);
        VoxelApp.LOGGER.debug("Decoded pack data (size: {} bytes)", data.length);
        return data;
    }

    @Contract(pure = true)
    public static @NotNull String createTagName(@NotNull String identifier) {
        return "pack-" + identifier.replace(":", "-");
    }

    public static void deleteExistingReleaseIfExists(String tag) throws IOException {
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

    private static @NotNull GHRelease createNewRelease(String tag, String identifier) throws IOException {
        VoxelApp.LOGGER.debug("Creating new release: {}", tag);
        String releaseName = "Pack for " + identifier;
        GHRelease release = repo.createRelease(tag)
                .name(releaseName)
                .prerelease(false)
                .create();
        VoxelApp.LOGGER.info("Created new release: {}", release.getHtmlUrl());
        return release;
    }

    private static void uploadAssetToRelease(@NotNull GHRelease release, String identifier, byte[] data) throws IOException {
        VoxelApp.LOGGER.debug("Uploading asset...");
        release.uploadAsset(
                identifier + ".zip",
                new ByteArrayInputStream(data),
                "application/zip"
        );
        VoxelApp.LOGGER.info("Asset uploaded successfully");
    }

    private static String findAssetDownloadUrl(@NotNull GHRelease release, String identifier) throws IOException {
        for (GHAsset asset : release.listAssets()) {
            if (asset.getName().equals(identifier.replace(":", ".") + ".zip")) {
                String url = asset.getBrowserDownloadUrl();
                VoxelApp.LOGGER.info("Asset download URL: {}", url);
                return url;
            }
        }
        VoxelApp.LOGGER.error("Uploaded asset not found in release assets");
        return "Asset upload failed or not found.";
    }
}