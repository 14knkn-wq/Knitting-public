package com.example.demo.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.config.ImageStorageProperties;
import com.example.demo.exception.ImageValidationException;
import com.example.demo.exception.ImageValidationException.Reason;

import net.coobird.thumbnailator.Thumbnails;

@Component
public class WorkImageStorage {
	
	private static final Logger logger =
	        LoggerFactory.getLogger(
	                WorkImageStorage.class
	        );

    private static final String WORKS_DIRECTORY_NAME = "works";

    private static final long MAX_FILE_SIZE =
            10L * 1024L * 1024L;

    private static final int MAX_IMAGE_DIMENSION = 8192;

    private final Path storageRoot;
    private final Path worksDirectory;
    
    private static final int MAX_SAVED_DIMENSION = 1920;

    private static final double JPEG_OUTPUT_QUALITY = 0.85;
    
    private static final Pattern PHOTO_REFERENCE_PATTERN =
            Pattern.compile(
                    "^works/"
                    + "[0-9a-f]{8}-"
                    + "[0-9a-f]{4}-"
                    + "[0-9a-f]{4}-"
                    + "[0-9a-f]{4}-"
                    + "[0-9a-f]{12}"
                    + "\\.(jpg|png)$"
            );
    
    private static final Duration STALE_TEMP_AGE =
            Duration.ofHours(24);

    private static final Pattern TEMP_FILE_PATTERN =
            Pattern.compile(
                    "^\\.[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.tmp$"
            );

    public WorkImageStorage(
            ImageStorageProperties properties) {

        String configuredRoot = properties.storageRoot();

        if (configuredRoot == null
                || configuredRoot.isBlank()) {
            throw new IllegalStateException(
                    "画像保存先が設定されていません。"
            );
        }

        try {
            this.storageRoot = Path.of(configuredRoot)
                    .toAbsolutePath()
                    .normalize();

            this.worksDirectory = storageRoot
                    .resolve(WORKS_DIRECTORY_NAME)
                    .normalize();

            initializeDirectory(storageRoot);
            initializeDirectory(worksDirectory);

        } catch (InvalidPathException e) {
            throw new IllegalStateException(
                    "画像保存先のパスが不正です。",
                    e
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "画像保存先を初期化できません。",
                    e
            );
        }
        
        cleanupStaleTempFiles();
        verifyStorageAccess();
    }

    public void validateUpload(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new ImageValidationException(
                    Reason.EMPTY_OR_UNREADABLE
            );
        }

        if (photo.getSize() > MAX_FILE_SIZE) {
            throw new ImageValidationException(
                    Reason.FILE_TOO_LARGE
            );
        }

        String expectedFormat =
                determineExpectedFormat(photo);

        try (InputStream inputStream =
                     photo.getInputStream();
             ImageInputStream imageInputStream =
                     ImageIO.createImageInputStream(
                             inputStream
                     )) {

            if (imageInputStream == null) {
                throw new ImageValidationException(
                        Reason.DECODE_FAILED
                );
            }

            Iterator<ImageReader> readers =
                    ImageIO.getImageReaders(
                            imageInputStream
                    );

            if (!readers.hasNext()) {
                throw new ImageValidationException(
                        Reason.DECODE_FAILED
                );
            }

            ImageReader reader = readers.next();

            try {
                String actualFormat =
                        reader.getFormatName()
                                .toUpperCase(Locale.ROOT);

                if (!actualFormat.equals(expectedFormat)) {
                    throw new ImageValidationException(
                            Reason.UNSUPPORTED_FORMAT
                    );
                }

                reader.setInput(
                        imageInputStream,
                        true,
                        true
                );

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width > MAX_IMAGE_DIMENSION
                        || height > MAX_IMAGE_DIMENSION) {
                    throw new ImageValidationException(
                            Reason.DIMENSIONS_TOO_LARGE
                    );
                }

                BufferedImage image = reader.read(0);

                if (image == null) {
                    throw new ImageValidationException(
                            Reason.DECODE_FAILED
                    );
                }

            } catch (IIOException e) {
                throw new ImageValidationException(
                        Reason.DECODE_FAILED
                );
            } finally {
                reader.dispose();
            }

        } catch (ImageValidationException e) {
            throw e;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "画像ファイルを読み込めません。",
                    e
            );
        }
    }
    
    public String saveNewImage(MultipartFile photo) {
        validateUpload(photo);

        String expectedFormat =
                determineExpectedFormat(photo);

        String uuid = UUID.randomUUID().toString();

        String extension =
                "JPEG".equals(expectedFormat)
                        ? ".jpg"
                        : ".png";

        Path tempFile;

        try {
            tempFile = writeProcessedImageToTemp(
                    photo,
                    expectedFormat,
                    uuid
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "画像を処理できません。",
                    e
            );
        }

        Path finalFile = worksDirectory
                .resolve(uuid + extension)
                .normalize();

        if (!finalFile.startsWith(worksDirectory)) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // best-effort cleanup
            }

            throw new IllegalStateException(
                    "画像保存先が不正です。"
            );
        }

        if (Files.exists(finalFile)) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // best-effort cleanup
            }

            throw new IllegalStateException(
                    "画像保存先が既に存在します。"
            );
        }

        try {
            try {
                Files.move(
                        tempFile,
                        finalFile,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(
                        tempFile,
                        finalFile
                );
            }

            return WORKS_DIRECTORY_NAME
                    + "/"
                    + uuid
                    + extension;

        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // best-effort cleanup
            }

            throw new IllegalStateException(
                    "画像を保存できません。",
                    e
            );
        }
    }
    
    public void deleteImage(String photoReference) {
        if (photoReference == null
                || !PHOTO_REFERENCE_PATTERN
                        .matcher(photoReference)
                        .matches()) {
            throw new IllegalStateException(
                    "画像参照が不正です。"
            );
        }

        Path targetFile = storageRoot
                .resolve(photoReference)
                .toAbsolutePath()
                .normalize();

        if (!targetFile.startsWith(worksDirectory)) {
            throw new IllegalStateException(
                    "画像保存領域外のパスです。"
            );
        }

        try {
            Files.deleteIfExists(targetFile);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "画像を削除できません。",
                    e
            );
        }
    }

    private String determineExpectedFormat(
            MultipartFile photo) {

        String originalFilename =
                photo.getOriginalFilename();

        String contentType =
                photo.getContentType();

        if (originalFilename == null
                || contentType == null) {
            throw new ImageValidationException(
                    Reason.UNSUPPORTED_FORMAT
            );
        }

        String lowerFilename =
                originalFilename.toLowerCase(
                        Locale.ROOT
                );

        if ((lowerFilename.endsWith(".jpg")
                || lowerFilename.endsWith(".jpeg"))
                && contentType.equals("image/jpeg")) {
            return "JPEG";
        }

        if (lowerFilename.endsWith(".png")
                && contentType.equals("image/png")) {
            return "PNG";
        }

        throw new ImageValidationException(
                Reason.UNSUPPORTED_FORMAT
        );
    }
    
    private Path writeProcessedImageToTemp(
            MultipartFile photo,
            String expectedFormat,
            String uuid) throws IOException {

        Path tempFile = worksDirectory
                .resolve("." + uuid + ".tmp")
                .normalize();

        try {
            BufferedImage orientedImage;

            try (InputStream inputStream =
                         photo.getInputStream()) {

                orientedImage = Thumbnails
                        .of(inputStream)
                        .scale(1.0)
                        .useExifOrientation(true)
                        .asBufferedImage();
            }

            if (orientedImage == null) {
                throw new ImageValidationException(
                        Reason.DECODE_FAILED
                );
            }

            BufferedImage processedImage =
                    orientedImage;

            int longestSide = Math.max(
                    orientedImage.getWidth(),
                    orientedImage.getHeight()
            );

            if (longestSide > MAX_SAVED_DIMENSION) {
                processedImage = Thumbnails
                        .of(orientedImage)
                        .size(
                                MAX_SAVED_DIMENSION,
                                MAX_SAVED_DIMENSION
                        )
                        .keepAspectRatio(true)
                        .asBufferedImage();
            }

            try (OutputStream outputStream =
                         Files.newOutputStream(
                                 tempFile,
                                 StandardOpenOption.CREATE_NEW,
                                 StandardOpenOption.WRITE
                         )) {

                if ("JPEG".equals(expectedFormat)) {
                    Thumbnails
                            .of(processedImage)
                            .scale(1.0)
                            .outputFormat("jpg")
                            .outputQuality(
                                    JPEG_OUTPUT_QUALITY
                            )
                            .toOutputStream(outputStream);

                } else if ("PNG".equals(expectedFormat)) {
                    Thumbnails
                            .of(processedImage)
                            .scale(1.0)
                            .outputFormat("png")
                            .toOutputStream(outputStream);

                } else {
                    throw new ImageValidationException(
                            Reason.UNSUPPORTED_FORMAT
                    );
                }
            }

            return tempFile;

        } catch (IOException
                | RuntimeException e) {

            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // best-effort cleanup
            }

            throw e;
        }
    }

    private void initializeDirectory(
            Path directory) throws IOException {

        if (Files.exists(directory)
                && !Files.isDirectory(directory)) {
            throw new IllegalStateException(
                    "画像保存先がディレクトリではありません。"
            );
        }

        Files.createDirectories(directory);

        if (!Files.isReadable(directory)
                || !Files.isWritable(directory)) {
            throw new IllegalStateException(
                    "画像保存先を読み書きできません。"
            );
        }
    }
    
    private boolean isStaleTempFile(
            Path path) {

        try {
            Instant lastModified =
                    Files.getLastModifiedTime(path)
                            .toInstant();

            Instant threshold =
                    Instant.now()
                            .minus(STALE_TEMP_AGE);

            return lastModified
                    .compareTo(threshold) <= 0;

        } catch (IOException e) {
            logger.warn(
                    "Failed to read temporary image file timestamp.",
                    e
            );

            return false;
        }
    }
    
    private void verifyStorageAccess() {

        if (!Files.isReadable(storageRoot)
                || !Files.isWritable(storageRoot)
                || !Files.isReadable(worksDirectory)
                || !Files.isWritable(worksDirectory)) {

            throw new IllegalStateException(
                    "Image storage directory is not readable or writable."
            );
        }
    }
    
    private void cleanupStaleTempFiles() {

        try (var paths =
                Files.list(worksDirectory)) {

            paths.filter(Files::isRegularFile)
                    .filter(path ->
                            TEMP_FILE_PATTERN
                                    .matcher(
                                            path.getFileName()
                                                    .toString()
                                    )
                                    .matches()
                    )
                    .filter(this::isStaleTempFile)
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);

                        } catch (IOException e) {
                            logger.warn(
                                    "Failed to delete stale temporary image file.",
                                    e
                            );
                        }
                    });

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to scan image storage directory.",
                    e
            );
        }
    }
    
    
}