package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.example.demo.config.ImageStorageProperties;
import com.example.demo.exception.ImageValidationException;
import com.example.demo.exception.ImageValidationException.Reason;

class WorkImageStorageTest {

    @TempDir
    Path tempDir;

    private WorkImageStorage workImageStorage;

    @BeforeEach
    void setUp() {
        workImageStorage = new WorkImageStorage(
                new ImageStorageProperties(
                        tempDir.toString()
                )
        );
    }

    @Test
    void 正常なJPEG画像はValidationを通過する()
            throws IOException {

        MockMultipartFile photo =
                new MockMultipartFile(
                        "photo",
                        "sample.jpg",
                        "image/jpeg",
                        createImageBytes(
                                "jpg",
                                100,
                                100
                        )
                );

        assertDoesNotThrow(
                () -> workImageStorage
                        .validateUpload(photo)
        );
    }

    @Test
    void 正常なPNG画像はValidationを通過する()
            throws IOException {

        MockMultipartFile photo =
                new MockMultipartFile(
                        "photo",
                        "sample.png",
                        "image/png",
                        createImageBytes(
                                "png",
                                100,
                                100
                        )
                );

        assertDoesNotThrow(
                () -> workImageStorage
                        .validateUpload(photo)
        );
    }

    @Test
    void 上限10MiBを超える画像はFILE_TOO_LARGEになる() {
        byte[] bytes =
                new byte[
                        10 * 1024 * 1024 + 1
                ];

        MockMultipartFile photo =
                new MockMultipartFile(
                        "photo",
                        "large.jpg",
                        "image/jpeg",
                        bytes
                );

        ImageValidationException exception =
                assertThrows(
                        ImageValidationException.class,
                        () -> workImageStorage
                                .validateUpload(photo)
                );

        assertEquals(
                Reason.FILE_TOO_LARGE,
                exception.getReason()
        );
    }

    @Test
    void JPEGとPNG以外はUNSUPPORTED_FORMATになる() {
        MockMultipartFile photo =
                new MockMultipartFile(
                        "photo",
                        "sample.gif",
                        "image/gif",
                        new byte[] {1, 2, 3}
                );

        ImageValidationException exception =
                assertThrows(
                        ImageValidationException.class,
                        () -> workImageStorage
                                .validateUpload(photo)
                );

        assertEquals(
                Reason.UNSUPPORTED_FORMAT,
                exception.getReason()
        );
    }

    @Test
    void 壊れた画像データはDECODE_FAILEDになる() {
        MockMultipartFile photo =
                new MockMultipartFile(
                        "photo",
                        "broken.png",
                        "image/png",
                        "not-an-image".getBytes()
                );

        ImageValidationException exception =
                assertThrows(
                        ImageValidationException.class,
                        () -> workImageStorage
                                .validateUpload(photo)
                );

        assertEquals(
                Reason.DECODE_FAILED,
                exception.getReason()
        );
    }

    @Test
    void 幅が8192pxを超える画像はDIMENSIONS_TOO_LARGEになる()
            throws IOException {

        MockMultipartFile photo =
                new MockMultipartFile(
                        "photo",
                        "large.png",
                        "image/png",
                        createImageBytes(
                                "png",
                                8193,
                                1
                        )
                );

        ImageValidationException exception =
                assertThrows(
                        ImageValidationException.class,
                        () -> workImageStorage
                                .validateUpload(photo)
                );

        assertEquals(
                Reason.DIMENSIONS_TOO_LARGE,
                exception.getReason()
        );
    }

    private byte[] createImageBytes(
            String format,
            int width,
            int height) throws IOException {

        int imageType =
                format.equals("jpg")
                        ? BufferedImage.TYPE_INT_RGB
                        : BufferedImage.TYPE_INT_ARGB;

        BufferedImage image =
                new BufferedImage(
                        width,
                        height,
                        imageType
                );

        try (ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream()) {

            boolean written =
                    ImageIO.write(
                            image,
                            format,
                            outputStream
                    );

            if (!written) {
                throw new IllegalStateException(
                        "テスト画像を生成できません。"
                );
            }

            return outputStream.toByteArray();
        }
    }
    
    @Test
    void JPEG画像を保存すると論理参照と正式ファイルが作成される()
            throws IOException {

        MockMultipartFile photo =
                new MockMultipartFile(
                        "photo",
                        "sample.jpg",
                        "image/jpeg",
                        createImageBytes(
                                "jpg",
                                100,
                                100
                        )
                );

        String photoReference =
                workImageStorage.saveNewImage(photo);

        assertTrue(
                photoReference.startsWith("works/")
        );

        assertTrue(
                photoReference.endsWith(".jpg")
        );

        Path savedFile =
                tempDir.resolve(photoReference);

        assertTrue(
                Files.exists(savedFile)
        );

        assertTrue(
                Files.isRegularFile(savedFile)
        );
    }
    
    @Test
    void 保存した画像を削除できる()
            throws IOException {

        MockMultipartFile photo =
                new MockMultipartFile(
                        "photo",
                        "sample.jpg",
                        "image/jpeg",
                        createImageBytes(
                                "jpg",
                                100,
                                100
                        )
                );

        String photoReference =
                workImageStorage.saveNewImage(photo);

        Path savedFile =
                tempDir.resolve(photoReference);

        assertTrue(Files.exists(savedFile));

        workImageStorage.deleteImage(
                photoReference
        );

        assertFalse(Files.exists(savedFile));
    }

    @Test
    void 不正な画像参照は削除できない() {
        assertThrows(
                IllegalStateException.class,
                () -> workImageStorage.deleteImage(
                        "../outside.jpg"
                )
        );
    }
    
    @Test
    void 起動時に24時間以上前の一時ファイルを削除する()
            throws Exception {

        Path worksDirectory =
                tempDir.resolve("works");

        Files.createDirectories(
                worksDirectory
        );

        Path staleTempFile =
                worksDirectory.resolve(
                        ".550e8400-e29b-41d4-a716-446655440000.tmp"
                );

        Files.write(
                staleTempFile,
                new byte[] {1, 2, 3}
        );

        Files.setLastModifiedTime(
                staleTempFile,
                FileTime.from(
                        Instant.now()
                                .minus(
                                        Duration.ofHours(25)
                                )
                )
        );

        new WorkImageStorage(
                new ImageStorageProperties(
                        tempDir.toString()
                )
        );

        assertFalse(
                Files.exists(
                        staleTempFile
                )
        );
    }
    
    @Test
    void 起動時に24時間未満の一時ファイルは削除しない()
            throws Exception {

        Path worksDirectory =
                tempDir.resolve("works");

        Files.createDirectories(
                worksDirectory
        );

        Path recentTempFile =
                worksDirectory.resolve(
                        ".550e8400-e29b-41d4-a716-446655440000.tmp"
                );

        Files.write(
                recentTempFile,
                new byte[] {1, 2, 3}
        );

        Files.setLastModifiedTime(
                recentTempFile,
                FileTime.from(
                        Instant.now()
                                .minus(
                                        Duration.ofHours(23)
                                )
                )
        );

        new WorkImageStorage(
                new ImageStorageProperties(
                        tempDir.toString()
                )
        );

        assertTrue(
                Files.exists(
                        recentTempFile
                )
        );
    }
    
    @Test
    void 起動時清掃で正式なJPEG画像は削除しない()
            throws Exception {

        Path worksDirectory =
                tempDir.resolve("works");

        Files.createDirectories(
                worksDirectory
        );

        Path imageFile =
                worksDirectory.resolve(
                        "550e8400-e29b-41d4-a716-446655440000.jpg"
                );

        Files.write(
                imageFile,
                new byte[] {1, 2, 3}
        );

        Files.setLastModifiedTime(
                imageFile,
                FileTime.from(
                        Instant.now()
                                .minus(
                                        Duration.ofHours(48)
                                )
                )
        );

        new WorkImageStorage(
                new ImageStorageProperties(
                        tempDir.toString()
                )
        );

        assertTrue(
                Files.exists(
                        imageFile
                )
        );
    }
    
    @Test
    void 長辺1920pxを超えるJPEGはアスペクト比を維持して縮小される()
            throws Exception {

        BufferedImage original =
                new BufferedImage(
                        4000,
                        3000,
                        BufferedImage.TYPE_INT_RGB
                );

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        ImageIO.write(
                original,
                "jpg",
                output
        );

        org.springframework.mock.web.MockMultipartFile photo =
                new org.springframework.mock.web.MockMultipartFile(
                        "photo",
                        "large.jpg",
                        "image/jpeg",
                        output.toByteArray()
                );

        WorkImageStorage storage =
                new WorkImageStorage(
                        new ImageStorageProperties(
                                tempDir.toString()
                        )
                );

        String photoReference =
                storage.saveNewImage(photo);

        BufferedImage saved =
                ImageIO.read(
                        tempDir.resolve(
                                photoReference
                        ).toFile()
                );

        assertEquals(
                1920,
                saved.getWidth()
        );

        assertEquals(
                1440,
                saved.getHeight()
        );
    }
    
    @Test
    void 長辺1920px以下のJPEGは拡大されない()
            throws Exception {

        BufferedImage original =
                new BufferedImage(
                        1200,
                        900,
                        BufferedImage.TYPE_INT_RGB
                );

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        ImageIO.write(
                original,
                "jpg",
                output
        );

        org.springframework.mock.web.MockMultipartFile photo =
                new org.springframework.mock.web.MockMultipartFile(
                        "photo",
                        "small.jpg",
                        "image/jpeg",
                        output.toByteArray()
                );

        WorkImageStorage storage =
                new WorkImageStorage(
                        new ImageStorageProperties(
                                tempDir.toString()
                        )
                );

        String photoReference =
                storage.saveNewImage(photo);

        BufferedImage saved =
                ImageIO.read(
                        tempDir.resolve(
                                photoReference
                        ).toFile()
                );

        assertEquals(
                1200,
                saved.getWidth()
        );

        assertEquals(
                900,
                saved.getHeight()
        );
    }
    
    @Test
    void PNG画像はPNGのまま保存される()
            throws Exception {

        BufferedImage original =
                new BufferedImage(
                        800,
                        600,
                        BufferedImage.TYPE_INT_ARGB
                );

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        ImageIO.write(
                original,
                "png",
                output
        );

        org.springframework.mock.web.MockMultipartFile photo =
                new org.springframework.mock.web.MockMultipartFile(
                        "photo",
                        "sample.png",
                        "image/png",
                        output.toByteArray()
                );

        WorkImageStorage storage =
                new WorkImageStorage(
                        new ImageStorageProperties(
                                tempDir.toString()
                        )
                );

        String photoReference =
                storage.saveNewImage(photo);

        assertTrue(
                photoReference.endsWith(".png")
        );

        Path savedPath =
                tempDir.resolve(
                        photoReference
                );

        BufferedImage saved =
                ImageIO.read(
                        savedPath.toFile()
                );

        assertNotNull(saved);

        assertEquals(
                800,
                saved.getWidth()
        );

        assertEquals(
                600,
                saved.getHeight()
        );
    }
    
    @Test
    void PNGとして送信されたJPEG画像は拒否する()
            throws Exception {

        BufferedImage original =
                new BufferedImage(
                        100,
                        100,
                        BufferedImage.TYPE_INT_RGB
                );

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        ImageIO.write(
                original,
                "jpg",
                output
        );

        org.springframework.mock.web.MockMultipartFile photo =
                new org.springframework.mock.web.MockMultipartFile(
                        "photo",
                        "sample.png",
                        "image/png",
                        output.toByteArray()
                );

        WorkImageStorage storage =
                new WorkImageStorage(
                        new ImageStorageProperties(
                                tempDir.toString()
                        )
                );

        ImageValidationException exception =
                assertThrows(
                        ImageValidationException.class,
                        () ->
                                storage.saveNewImage(
                                        photo
                                )
                );

        assertEquals(
                ImageValidationException.Reason.UNSUPPORTED_FORMAT,
                exception.getReason()
        );
    }
    
    @Test
    void 幅8192pxちょうどの画像は許可される()
            throws Exception {

        BufferedImage original =
                new BufferedImage(
                        8192,
                        1,
                        BufferedImage.TYPE_INT_RGB
                );

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        ImageIO.write(
                original,
                "png",
                output
        );

        org.springframework.mock.web.MockMultipartFile photo =
                new org.springframework.mock.web.MockMultipartFile(
                        "photo",
                        "boundary.png",
                        "image/png",
                        output.toByteArray()
                );

        WorkImageStorage storage =
                new WorkImageStorage(
                        new ImageStorageProperties(
                                tempDir.toString()
                        )
                );

        assertDoesNotThrow(
                () -> storage.saveNewImage(photo)
        );
    }
    
    @Test
    void ゼロbyte画像は読み込み不能として拒否する() {

        org.springframework.mock.web.MockMultipartFile photo =
                new org.springframework.mock.web.MockMultipartFile(
                        "photo",
                        "empty.jpg",
                        "image/jpeg",
                        new byte[0]
                );

        WorkImageStorage storage =
                new WorkImageStorage(
                        new ImageStorageProperties(
                                tempDir.toString()
                        )
                );

        ImageValidationException exception =
                assertThrows(
                        ImageValidationException.class,
                        () -> storage.saveNewImage(photo)
                );

        assertEquals(
                ImageValidationException.Reason.EMPTY_OR_UNREADABLE,
                exception.getReason()
        );
    }
    
    @Test
    void storageRootが通常ファイルの場合は起動失敗する()
            throws Exception {

        Path rootFile =
                tempDir.resolve(
                        "image-storage"
                );

        Files.writeString(
                rootFile,
                "not a directory"
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        new WorkImageStorage(
                                new ImageStorageProperties(
                                        rootFile.toString()
                                )
                        )
        );
    }
    
    @Test
    void worksが通常ファイルの場合は起動失敗する()
            throws Exception {

        Path storageRoot =
                tempDir.resolve(
                        "image-storage"
                );

        Files.createDirectories(
                storageRoot
        );

        Files.writeString(
                storageRoot.resolve("works"),
                "not a directory"
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        new WorkImageStorage(
                                new ImageStorageProperties(
                                        storageRoot.toString()
                                )
                        )
        );
    }
    
    @Test
    void storageRootが空文字の場合は起動失敗する() {

        assertThrows(
                IllegalStateException.class,
                () ->
                        new WorkImageStorage(
                                new ImageStorageProperties(
                                        ""
                                )
                        )
        );
    }
    
    @Test
    void 画像容量がちょうど10MiBなら許可する()
            throws Exception {

        BufferedImage image =
                new BufferedImage(
                        100,
                        100,
                        BufferedImage.TYPE_INT_RGB
                );

        java.io.ByteArrayOutputStream output =
                new java.io.ByteArrayOutputStream();

        ImageIO.write(
                image,
                "jpg",
                output
        );

        byte[] exact10MiB =
                java.util.Arrays.copyOf(
                        output.toByteArray(),
                        10 * 1024 * 1024
                );

        MockMultipartFile photo =
                new MockMultipartFile(
                        "photo",
                        "sample.jpg",
                        "image/jpeg",
                        exact10MiB
                );

        WorkImageStorage storage =
                new WorkImageStorage(
                        new ImageStorageProperties(
                                tempDir.toString()
                        )
                );

        assertDoesNotThrow(
                () -> storage.saveNewImage(photo)
        );
    }
    
    @Test
    void 画像容量が10MiBを1byte超えたら拒否する()
            throws Exception {

        byte[] over10MiB =
                new byte[
                        10 * 1024 * 1024 + 1
                ];

        MockMultipartFile photo =
                new MockMultipartFile(
                        "photo",
                        "sample.jpg",
                        "image/jpeg",
                        over10MiB
                );

        WorkImageStorage storage =
                new WorkImageStorage(
                        new ImageStorageProperties(
                                tempDir.toString()
                        )
                );

        ImageValidationException exception =
                assertThrows(
                        ImageValidationException.class,
                        () -> storage.saveNewImage(photo)
                );

        assertEquals(
                ImageValidationException.Reason.FILE_TOO_LARGE,
                exception.getReason()
        );
    }
    
    @Test
    void EXIFOrientation6を保存時に補正する()
            throws Exception {

        byte[] imageBytes =
                Files.readAllBytes(
                        Path.of(
                                "src/test/resources/images/fixtures/orientation-6.jpg"
                        )
                );

        MockMultipartFile photo =
                new MockMultipartFile(
                        "photo",
                        "orientation-6.jpg",
                        "image/jpeg",
                        imageBytes
                );

        WorkImageStorage storage =
                new WorkImageStorage(
                        new ImageStorageProperties(
                                tempDir.toString()
                        )
                );

        String reference =
                storage.saveNewImage(photo);

        BufferedImage savedImage =
                ImageIO.read(
                        tempDir.resolve(reference)
                                .toFile()
                );

        assertEquals(
                40,
                savedImage.getWidth()
        );

        assertEquals(
                80,
                savedImage.getHeight()
        );
    }
    
    @Test
    void 保存後JPEGには元のEXIFメタデータを残さない()
            throws Exception {

        byte[] imageBytes =
                Files.readAllBytes(
                        Path.of(
                                "src/test/resources/images/fixtures/metadata-bearing.jpg"
                        )
                );

        MockMultipartFile photo =
                new MockMultipartFile(
                        "photo",
                        "metadata-bearing.jpg",
                        "image/jpeg",
                        imageBytes
                );

        WorkImageStorage storage =
                new WorkImageStorage(
                        new ImageStorageProperties(
                                tempDir.toString()
                        )
                );

        String reference =
                storage.saveNewImage(photo);

        byte[] savedBytes =
                Files.readAllBytes(
                        tempDir.resolve(reference)
                );

        String savedContent =
                new String(
                        savedBytes,
                        java.nio.charset.StandardCharsets.ISO_8859_1
                );

        assertFalse(
                savedContent.contains(
                        "KnittingTestCamera"
                )
        );

        assertFalse(
                savedContent.contains(
                        "KnittingTestModel"
                )
        );

        assertFalse(
                savedContent.contains(
                        "2026:10:01 12:00:00"
                )
        );
    }
    
    @Test
    void UUID形式ではない古いtmpは削除しない()
            throws Exception {

        Path storageRoot =
                tempDir.resolve(
                        "image-storage"
                );

        Path worksDirectory =
                storageRoot.resolve(
                        "works"
                );

        Files.createDirectories(
                worksDirectory
        );

        Path unrelatedTmp =
                worksDirectory.resolve(
                        ".not-a-uuid.tmp"
                );

        Files.writeString(
                unrelatedTmp,
                "test"
        );

        Files.setLastModifiedTime(
                unrelatedTmp,
                java.nio.file.attribute.FileTime.from(
                        java.time.Instant.now()
                                .minus(
                                        java.time.Duration.ofHours(48)
                                )
                )
        );

        new WorkImageStorage(
                new ImageStorageProperties(
                        storageRoot.toString()
                )
        );

        assertTrue(
                Files.exists(
                        unrelatedTmp
                )
        );
    }
    
    @Test
    void default画像は削除対象にできない() {

        WorkImageStorage storage =
                new WorkImageStorage(
                        new ImageStorageProperties(
                                tempDir.toString()
                        )
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        storage.deleteImage(
                                "images/default-work.png"
                        )
        );
    }
}