package com.jonasjschreiber.fileengine.service;

import com.jonasjschreiber.fileengine.controller.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

@Service
@Slf4j
public class FileService {

    @Value("${app.absolute.upload.path:${user.home}}")
    public String uploadDir;

    protected final static List<String> ACCEPTABLE_TYPES = Arrays.asList("jpg", "png", "mp4", "mov", "avi", "bmp");

    public void uploadFile(MultipartFile file) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(file.getOriginalFilename()) ||
                !isAcceptableType(file.getOriginalFilename())) {
            log.error("Filetype: {} is not an acceptable type",
                    org.apache.commons.lang3.StringUtils.isEmpty(file.getOriginalFilename()) ?
                            file.getOriginalFilename() :
                            file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1));
            throw new FileStorageException("Is not an acceptable type");
        }
        try {
            String filename = uploadDir + StringUtils.cleanPath(file.getOriginalFilename());
            if (Files.exists(Paths.get(filename))) {
                String random = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
                String newFilename = uploadDir + StringUtils.cleanPath(file.getOriginalFilename()
                                .substring(0, file.getOriginalFilename().indexOf("."))
                                + "-" + random
                                + file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")));

                log.info("File already exists: {}, renaming: {}", filename, newFilename);
                filename = newFilename;
            }
            Files.copy(file.getInputStream(), Paths.get(filename));
            log.info("Uploaded File to: {}", filename);
        } catch (Exception e) {
            log.error("Exception: ", e);
            throw new FileStorageException("Could not store file " + file.getOriginalFilename()
                + ". Please try again!");
        }
    }

    public List<String> getFileNames() throws IOException {
        List<String> filenames = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(uploadDir))) {
            paths.filter(Files::isRegularFile).forEach(f -> filenames.add(f.getFileName().toString()));
        }
        return filenames;
    }

    private boolean isAcceptableType(String filename) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(filename)) return false;
        if (filename.lastIndexOf("." ) <= 0 || filename.lastIndexOf(".") >= filename.length()) return false;
        return ACCEPTABLE_TYPES.contains(filename.toLowerCase().substring(filename.lastIndexOf("." ) + 1));
    }
}
