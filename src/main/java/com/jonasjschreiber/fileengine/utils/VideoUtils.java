package com.jonasjschreiber.fileengine.utils;

import net.coobird.thumbnailator.Thumbnails;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class VideoUtils {

    private static final String IMAGEMAT = "png";
    public static final List<String> VIDEO_TYPES = Arrays.asList("mp4", "mov", "avi");

    public static void generateVideoThumbnail(String uploadDir, String filename) throws Exception {
        FFmpegFrameGrabber ff = FFmpegFrameGrabber.createDefault(filename);
        String thumbnailPath = uploadDir + "thumbs" + File.separator +
                ImageUtils.getBaseNameOfFile(filename) + "." + ImageUtils.getExtension(filename);
        ff.start();
        int ffLength = ff.getLengthInFrames();
        Frame f;
        int i = 0;
        while (i++ < ffLength) {
            f = ff.grabImage();
            if (i == 5) {
                doExecuteFrame(f, thumbnailPath);
                break;
            }
        }
        ff.stop();
    }

    /**
     * Capture thumbnails
     * @param f
     * @param targetFilePath: cover image
     */
    public static void doExecuteFrame(Frame f, String targetFilePath) throws IOException {
        if (null == f || null == f.image) {
            return;
        }
        Java2DFrameConverter converter = new Java2DFrameConverter();
        BufferedImage image = ImageUtils.resize(converter.getBufferedImage(f), 200);
        File output = new File(targetFilePath);
        ImageIO.write(image, IMAGEMAT, output);
    }

    public static boolean isVideoType(String filename) {
        return VIDEO_TYPES.contains(ImageUtils.getExtension(filename));
    }
}
