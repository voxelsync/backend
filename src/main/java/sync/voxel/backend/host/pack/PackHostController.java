package sync.voxel.backend.host.pack;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sync.voxel.backend.VoxelApp;

@RestController
public class PackHostController {
    private final PackHostManager hostManager = new PackHostManager();

    @PostMapping(VoxelApp.PACK_UPLOAD_URL + "{hostKey}/register")
    public ResponseEntity<String> registerHostKey(
            @RequestPart String hostKey,
            @RequestParam String adminAuth) {

        if (!hostManager.isAdminAuthorized(adminAuth)) {
            VoxelApp.LOGGER.warn("Unauthorized registration attempt for {}", hostKey);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
        }

        hostManager.registerHostKey(hostKey);
        return ResponseEntity.accepted().body("Host key registered");
    }

    @PostMapping(VoxelApp.PACK_UPLOAD_URL + "{hostKey}/remove")
    public ResponseEntity<String> removeHostKey(
            @RequestPart String hostKey,
            @RequestParam String adminAuth) {

        if (!hostManager.isAdminAuthorized(adminAuth)) {
            VoxelApp.LOGGER.warn("Unauthorized remove attempt for {}", hostKey);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
        }

        hostManager.removeHostKey(hostKey);
        return ResponseEntity.accepted().body("Host key removed");
    }

    @PostMapping(VoxelApp.PACK_UPLOAD_URL + "{hostKey}/upload")
    public ResponseEntity<String> uploadPack(
            @RequestPart String hostKey,
            @RequestParam String identifier,
            @RequestParam String base64Pack) {

        VoxelApp.LOGGER.debug("Upload request for {} [{}]", hostKey, identifier);

        if (hostKey == null || identifier == null || base64Pack == null) {
            return ResponseEntity.badRequest().body("Missing parameters");
        }

        String url = hostManager.handlePackUpload(hostKey, identifier, base64Pack);
        if (url == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Identifier mismatch");
        }
        return ResponseEntity.ok(url);
    }

    @PostMapping(VoxelApp.PACK_UPLOAD_URL + "{hostKey}/match")
    public ResponseEntity<Boolean> verifyIdentifierMatch(
            @RequestPart String hostKey,
            @RequestParam String identifier) {

        VoxelApp.LOGGER.debug("Verification request for {} [{}]", hostKey, identifier);

        if (hostKey == null || identifier == null) {
            return ResponseEntity.ok(false);
        }

        boolean matches = hostManager.isKeyMatchToIdentifier(hostKey, identifier);

        return ResponseEntity.ok(matches);
    }

    @PostMapping(VoxelApp.PACK_UPLOAD_URL + "{hostKey}/identifier/replace")
    public ResponseEntity<Void> replaceIdentifierOfHostKey(
            @RequestPart String hostKey,
            @RequestParam String identifier,
            @RequestParam String oldIdentifier) {

        VoxelApp.LOGGER.debug("Release request for {} [{}]", hostKey, identifier);

        if (hostKey == null || identifier == null) {
            return ResponseEntity.badRequest().build();
        }

        hostManager.rotateIdentifierForHostKey(hostKey, oldIdentifier, identifier);
        return ResponseEntity.noContent().build();
    }
}