package com.example.demo.service;

import com.example.demo.dto.wiki.EnchantmentDto;
import com.example.demo.dto.wiki.SpellDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

@Service
public class WikiJsonStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WikiJsonStorageService.class);
    private static final String CLASSPATH_DATA_ROOT = "static/data/";
    private static final List<Path> DEV_DATA_DIR_CANDIDATES = List.of(
            Paths.get("src/main/resources/static/data"),
            Paths.get("demo/src/main/resources/static/data")
    );

    private final ObjectMapper objectMapper;
    private final String configuredDataDir;

    public WikiJsonStorageService(ObjectMapper objectMapper,
                                  @Value("${atg.data.dir:}") String configuredDataDir) {
        this.objectMapper = objectMapper;
        this.configuredDataDir = configuredDataDir;
    }

    public List<SpellDto> readSpells() {
        return readList("spells.json", SpellDto[].class);
    }

    public void writeSpells(List<SpellDto> spells) {
        writeList("spells.json", spells);
    }

    public List<EnchantmentDto> readEnchantments() {
        return readList("enchantments.json", EnchantmentDto[].class);
    }

    public void writeEnchantments(List<EnchantmentDto> enchantments) {
        writeList("enchantments.json", enchantments);
    }

    private <T> List<T> readList(String fileName, Class<T[]> arrayType) {
        Path externalPath = resolveExternalFile(fileName);
        if (externalPath != null && Files.isRegularFile(externalPath)) {
            List<T> externalData = readExternalFile(externalPath, arrayType);
            if (externalData != null) {
                return externalData;
            }
        }

        return readBundledFile(fileName, arrayType);
    }

    private <T> List<T> readExternalFile(Path filePath, Class<T[]> arrayType) {
        try (InputStream inputStream = Files.newInputStream(filePath, StandardOpenOption.READ)) {
            return readArray(inputStream, arrayType);
        } catch (Exception exception) {
            LOGGER.warn("Cannot read external wiki data file {}. Falling back to bundled data.", filePath, exception);
            return null;
        }
    }

    private <T> List<T> readBundledFile(String fileName, Class<T[]> arrayType) {
        ClassPathResource resource = new ClassPathResource(CLASSPATH_DATA_ROOT + fileName);
        if (!resource.exists()) {
            return List.of();
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return readArray(inputStream, arrayType);
        } catch (Exception exception) {
            LOGGER.warn("Cannot read bundled wiki data resource {}", fileName, exception);
            return List.of();
        }
    }

    private <T> List<T> readArray(InputStream inputStream, Class<T[]> arrayType) throws IOException {
        T[] values = objectMapper.readValue(inputStream, arrayType);
        return values == null ? List.of() : Arrays.asList(values);
    }

    private void writeList(String fileName, Object payload) {
        Path targetFile = resolveWritableFile(fileName);
        try {
            Files.createDirectories(targetFile.getParent());
            Path tempFile = Files.createTempFile(targetFile.getParent(), fileName + "-", ".tmp");
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload)
                    + System.lineSeparator();
            Files.writeString(
                    tempFile,
                    json,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            moveTempFile(tempFile, targetFile);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write wiki JSON file " + targetFile, exception);
        }
    }

    private void moveTempFile(Path tempFile, Path targetFile) throws IOException {
        try {
            Files.move(
                    tempFile,
                    targetFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path resolveWritableFile(String fileName) {
        Path baseDir = resolveBaseDataDir();
        if (baseDir == null) {
            throw new IllegalStateException(
                    "Wiki JSON path is not writable. Configure atg.data.dir or run from the project source tree."
            );
        }
        return baseDir.resolve(fileName);
    }

    private Path resolveExternalFile(String fileName) {
        Path baseDir = resolveBaseDataDir();
        return baseDir == null ? null : baseDir.resolve(fileName);
    }

    private Path resolveBaseDataDir() {
        if (StringUtils.hasText(configuredDataDir)) {
            return Paths.get(configuredDataDir.trim()).toAbsolutePath().normalize();
        }

        for (Path candidate : DEV_DATA_DIR_CANDIDATES) {
            Path absoluteCandidate = candidate.toAbsolutePath().normalize();
            Path parent = absoluteCandidate.getParent();
            if (Files.isDirectory(absoluteCandidate) || (parent != null && Files.isDirectory(parent))) {
                return absoluteCandidate;
            }
        }

        return null;
    }
}
