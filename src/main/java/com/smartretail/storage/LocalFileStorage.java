package com.smartretail.storage;

import com.smartretail.common.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Development storage on the local disk. Replace with an S3/MinIO implementation for production. */
@Service
public class LocalFileStorage implements StorageService {

    static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    /** Keys this class hands out for private files: folder/uuid.ext. Anything else is rejected. */
    private static final Pattern PRIVATE_KEY = Pattern.compile("[a-z]+/[0-9a-f-]{36}\\.(jpg|png|webp)");

    private final Path root;
    private final Path privateRoot;
    private final String publicPath;

    public LocalFileStorage(StorageProperties properties) {
        this.root = Path.of(properties.localDir()).toAbsolutePath().normalize();
        this.privateRoot = Path.of(properties.privateDir()).toAbsolutePath().normalize();
        this.publicPath = properties.publicPath();
        try {
            Files.createDirectories(root);
            Files.createDirectories(privateRoot);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create upload folders " + root + ", " + privateRoot, e);
        }
    }

    public Path root() {
        return root;
    }

    @Override
    public String storeImage(MultipartFile file, String folder) {
        return publicPath + "/" + write(root, file, folder);
    }

    @Override
    public String storePrivateImage(MultipartFile file, String folder) {
        return write(privateRoot, file, folder);
    }

    @Override
    public Optional<Path> privateFile(String key) {
        if (key == null || !PRIVATE_KEY.matcher(key).matches()) {
            return Optional.empty();
        }
        Path file = privateRoot.resolve(key).normalize();
        return file.startsWith(privateRoot) && Files.isRegularFile(file) ? Optional.of(file) : Optional.empty();
    }

    /** Saves an image under {@code base/folder} and returns "folder/name". */
    private static String write(Path base, MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Choose an image to upload");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw ApiException.badRequest("Image must be 5 MB or smaller");
        }
        try {
            // The extension comes from the file's content, never from the name the client sent.
            String extension = detectImageExtension(file);
            Path dir = base.resolve(folder).normalize();
            if (!dir.startsWith(base)) {
                throw new IllegalArgumentException("Invalid folder: " + folder);
            }
            Files.createDirectories(dir);
            String fileName = UUID.randomUUID() + "." + extension;
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
            return folder + "/" + fileName;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store upload", e);
        }
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith(publicPath + "/")) {
            return;
        }
        Path file = root.resolve(url.substring(publicPath.length() + 1)).normalize();
        if (!file.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete " + url, e);
        }
    }

    private static String detectImageExtension(MultipartFile file) throws IOException {
        byte[] head;
        try (InputStream in = file.getInputStream()) {
            head = in.readNBytes(12);
        }
        if (startsWith(head, 0xFF, 0xD8, 0xFF)) {
            return "jpg";
        }
        if (startsWith(head, 0x89, 'P', 'N', 'G')) {
            return "png";
        }
        if (head.length >= 12 && startsWith(head, 'R', 'I', 'F', 'F')
                && Arrays.equals(Arrays.copyOfRange(head, 8, 12), new byte[] {'W', 'E', 'B', 'P'})) {
            return "webp";
        }
        throw ApiException.badRequest("Only JPG, PNG or WebP images are allowed");
    }

    private static boolean startsWith(byte[] data, int... prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
