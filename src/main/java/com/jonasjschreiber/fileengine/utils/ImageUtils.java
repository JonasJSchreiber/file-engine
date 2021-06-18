package com.jonasjschreiber.fileengine.utils;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class ImageUtils {

    protected final static List<String> ACCEPTABLE_TYPES = Arrays.asList("jpg", "jpeg", "png", "mp4", "mov", "avi", "bmp");

    public static void generateThumbnail(String uploadDir, String filename) throws Exception {
        String thumbnailPath = getThumbnailPath(uploadDir, filename);
        BufferedImage img = ImageIO.read(new File(filename));
        BufferedImage scaledImg = Thumbnails.of(new FileInputStream(filename)).scale((double) 200/img.getWidth()).asBufferedImage();
        ImageIO.write(scaledImg, getExtension(filename), new File(thumbnailPath));
        log.info("Wrote thumbnail to: {}", thumbnailPath);
    }

    public static String getThumbnailPath(String uploadDir, String filename) {
        return uploadDir + "thumbs" + File.separator +
                getBaseNameOfFile(filename) + "." + getExtension(filename);
    }

    public static boolean isAcceptableType(String filename) {
        if (filename == null || filename.length() == 0) return false;
        return ACCEPTABLE_TYPES.contains(getExtension(filename.toLowerCase()));
    }

    public static String getBaseNameOfFile(String filename) {
        return FilenameUtils.getBaseName(filename);
    }

    public static String getExtension(String filename) {
        return FilenameUtils.getExtension(filename);
    }

    public static BufferedImage resize(BufferedImage img, int width) throws IOException {
        return Thumbnails.of(img).width(width).asBufferedImage();
    }
}
