package com.jonasjschreiber.fileengine.service;

import com.jonasjschreiber.fileengine.controller.FileStorageException;
import com.jonasjschreiber.fileengine.model.Image;
import com.jonasjschreiber.fileengine.utils.ImageUtils;
import com.jonasjschreiber.fileengine.utils.VideoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileService {

    @Value("${app.absolute.upload.path:${user.home}/tmp}")
    public String uploadDir;

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
        if (!ImageUtils.isAcceptableType(file.getOriginalFilename())) {
            log.error("Filetype: {} is not an acceptable type", ImageUtils.getExtension(file.getName()));
            throw new FileStorageException(MessageFormat.format(
                    "%s not an acceptable type", file.getName()));
        }
        try {
            String filename = uploadDir + StringUtils.cleanPath(file.getOriginalFilename());
            if (Files.exists(Paths.get(filename))) {
                String random = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
                String newFilename = uploadDir + StringUtils.cleanPath(ImageUtils.getBaseNameOfFile(filename)
                        + "-" + random + "." + ImageUtils.getExtension(filename));
                log.info("File already exists: {}, renaming: {}", filename, newFilename);
                filename = newFilename;
            }
            Files.copy(file.getInputStream(), Paths.get(filename));
            if (VideoUtils.isVideoType(filename)) {
                VideoUtils.generateVideoThumbnailXuggle(uploadDir, filename);
            } else {
                ImageUtils.generateThumbnail(uploadDir, filename);
            }

            log.info("Uploaded File to: {}", filename);
        } catch (Exception e) {
            log.error("Exception: ", e);
            throw new FileStorageException("Could not store file " + file.getOriginalFilename()
                + ". Please try again!");
        }
    }

    public List<Image> getList() throws IOException {
        List<File> files = Files.walk(Paths.get(uploadDir))
                .filter(Files::isRegularFile)
                .filter(f -> ImageUtils.isAcceptableType(f.getFileName().toString()))
                .map(p -> new File(String.valueOf(p.getFileName())))
                .collect(Collectors.toList());
        return files.stream()
                .sorted(Comparator.comparingLong(File::lastModified).reversed())
                .map(f -> {
                    String filename = f.getName();
                    Image image = com.jonasjschreiber.fileengine.model.Image.builder()
                            .filename(ImageUtils.getBaseNameOfFile(filename) + "." + ImageUtils.getExtension(filename))
                            .thumbnailName("thumbs/" + ImageUtils.getBaseNameOfFile(filename))
                            .thumbnailUrl(ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
                                    + "/files/getThumbnail?filename=" + filename)
                            .url(ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
                                    + (VideoUtils.isVideoType(filename) ?
                                        "/files/getVideo?filename=" : "/files/getImage?filename=")
                                    + filename)
                            .build();
                    return image;
                })
                .distinct()
                .collect(Collectors.toList());
    }
}
