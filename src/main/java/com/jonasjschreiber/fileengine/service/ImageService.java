package com.jonasjschreiber.fileengine.service;

import com.jonasjschreiber.fileengine.controller.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ImageService {

    @Value("${app.absolute.upload.path:${user.home}/tmp}")
    public String uploadDir;

    protected final static List<String> ACCEPTABLE_TYPES = Arrays.asList("jpg", "png", "mp4", "mov", "avi", "bmp");

    public byte[] getFile(String filename) {
        log.info("Attempting to retrieve: {}", StringUtils.cleanPath(filename));
        try {
            InputStream inputStream = new FileInputStream(StringUtils.cleanPath(filename));
            return StreamUtils.copyToByteArray(inputStream);
        } catch (Exception e) {
            log.error("Exception: ", e);
        }
        return null;
    }

    public void uploadFile(MultipartFile file) {
        log.info("Filename: {}", file.getOriginalFilename());
        if (!isAcceptableType(file.getOriginalFilename())) {
            log.error("Filetype: {} is not an acceptable type", getExtensionByApacheCommonLib(file.getName()));
            throw new FileStorageException(MessageFormat.format(
                    "%s not an acceptable type", file.getName()));
        }
        try {
            String filename = uploadDir + StringUtils.cleanPath(file.getOriginalFilename());
            if (Files.exists(Paths.get(filename))) {
                String random = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
                String newFilename = uploadDir + StringUtils.cleanPath(getBaseNameOfFile(filename)
                        + "-" + random + "." + getExtensionByApacheCommonLib(filename));
                log.info("File already exists: {}, renaming: {}", filename, newFilename);
                filename = newFilename;
            }
            Files.copy(file.getInputStream(), Paths.get(filename));
            generateThumbnail(filename);
            log.info("Uploaded File to: {}", filename);
        } catch (Exception e) {
            log.error("Exception: ", e);
            throw new FileStorageException("Could not store file " + file.getOriginalFilename()
                + ". Please try again!");
        }
    }

    private void generateThumbnail(String filename) throws IOException {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        img.createGraphics().drawImage(ImageIO.read(new File(filename)).getScaledInstance(
                100, 100, Image.SCALE_SMOOTH),0,0,null);
        String thumbnailPath = getThumbnailPath(filename);
        ImageIO.write(img, getExtensionByApacheCommonLib(filename), new File(thumbnailPath));
        log.info("Wrote thumbnail to: {}", thumbnailPath);
    }

    private String getThumbnailPath(String filename) {
        return uploadDir + "thumbs" + File.separator +
                getBaseNameOfFile(filename) + "." + getExtensionByApacheCommonLib(filename);
    }

    public Set<com.jonasjschreiber.fileengine.model.Image> getList() throws IOException {
        return Files.walk(Paths.get(uploadDir))
                .filter(Files::isRegularFile)
                .filter(f -> isAcceptableType(f.getFileName().toString()))
                .map(f -> {
                    String filename = f.getFileName().toString();
                    com.jonasjschreiber.fileengine.model.Image image = com.jonasjschreiber.fileengine.model.Image.builder()
                            .filename(getBaseNameOfFile(filename) + "." + getExtensionByApacheCommonLib(filename))
                            .thumbnailName("thumbs/" + (getBaseNameOfFile(filename)))
                            .thumbnailUrl(ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
                                    + "/images/getThumbnail?filename=" + filename)
                            .url(ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
                                    + "/images/getImage?filename=" + filename)
                            .build();
                    return image;
                }).collect(Collectors.toSet());
    }

    private boolean isAcceptableType(String filename) {
        if (filename == null || filename.length() == 0) return false;
        return ACCEPTABLE_TYPES.contains(getExtensionByApacheCommonLib(filename.toLowerCase()));
    }

    private String getBaseNameOfFile(String filename) {
        return FilenameUtils.getBaseName(filename);
    }

    public String getExtensionByApacheCommonLib(String filename) {
        return FilenameUtils.getExtension(filename);
    }
}
