package com.jonasjschreiber.fileengine.utils;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class ImageUtils {

    protected final static List<String> ACCEPTABLE_TYPES = Arrays.asList("jpg", "png", "mp4", "mov", "avi", "bmp");

    public static void generateThumbnail(String uploadDir, String filename) throws Exception {
        BufferedImage img = ImageIO.read(new File(filename));
        BufferedImage scaledImg = Thumbnails.of(new FileInputStream(filename)).scale(0.05).asBufferedImage();
        String thumbnailPath = getThumbnailPath(uploadDir, filename);
        ImageIO.write(scaledImg, getExtensionByApacheCommonLib(filename), new File(thumbnailPath));
        log.info("Wrote thumbnail to: {}", thumbnailPath);
    }

    public static String getThumbnailPath(String uploadDir, String filename) {
        return uploadDir + "thumbs" + File.separator +
                getBaseNameOfFile(filename) + "." + getExtensionByApacheCommonLib(filename);
    }

    public static boolean isAcceptableType(String filename) {
        if (filename == null || filename.length() == 0) return false;
        return ACCEPTABLE_TYPES.contains(getExtensionByApacheCommonLib(filename.toLowerCase()));
    }

    public static String getBaseNameOfFile(String filename) {
        return FilenameUtils.getBaseName(filename);
    }

    public static String getExtensionByApacheCommonLib(String filename) {
        return FilenameUtils.getExtension(filename);
    }
}
