package com.smartretail.storage;

import java.nio.file.Path;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

/** Stores uploaded images: public ones (product photos, banners) and private ones (payment proofs). */
public interface StorageService {

    /** Saves an image under {@code folder} and returns its public path, e.g. "/uploads/products/ab12.jpg". */
    String storeImage(MultipartFile file, String folder);

    /**
     * Saves an image that must not be publicly reachable and returns its key, e.g. "payments/ab12.png".
     * Serve it only through an endpoint that checks who is asking.
     */
    String storePrivateImage(MultipartFile file, String folder);

    /** The file for a key from {@link #storePrivateImage}, or empty if the key is unknown or malformed. */
    Optional<Path> privateFile(String key);

    /** Deletes a file previously returned by {@link #storeImage}; ignores URLs it does not own. */
    void delete(String url);
}
