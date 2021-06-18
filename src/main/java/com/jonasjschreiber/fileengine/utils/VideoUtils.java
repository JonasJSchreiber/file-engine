package com.jonasjschreiber.fileengine.utils;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.Global;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class VideoUtils {

    public static final List<String> VIDEO_TYPES = Arrays.asList("mp4", "mov", "avi");

    public static boolean isVideoType(String filename) {
        return VIDEO_TYPES.contains(ImageUtils.getExtension(filename));
    }

    public static final double SECONDS_BETWEEN_FRAMES = 1;

    private static String inputFilename = null;
    private static String outputFilePrefix = null;
    private static String outputFilename = null;

    // The video stream index, used to ensure we display frames from one and
    // only one video stream from the media container.
    private static int mVideoStreamIndex = -1;

    // Time of last frame write
    private static long mLastPtsWrite = Global.NO_PTS;

    public static final long MICRO_SECONDS_BETWEEN_FRAMES = (long) (Global.DEFAULT_PTS_PER_SECOND * SECONDS_BETWEEN_FRAMES);


    /**
     * Generates Thumbnails for the given Video and uploads it to i5 cloud object store.
     *
     * @param uploadDir Path to the file.
     * @param filename  Name of the file.
     * @return String URL to uploaded Thumbnail.
     */
    public static void generateVideoThumbnailXuggle(String uploadDir, String filename) {
        mVideoStreamIndex = -1;
        mLastPtsWrite = Global.NO_PTS;

        outputFilePrefix = uploadDir;
        IMediaReader mediaReader = ToolFactory.makeReader(filename);

        try {
            mediaReader
                    .setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
            ImageSnapListener isListener = new ImageSnapListener(uploadDir, ImageUtils.getBaseNameOfFile(filename),
                    ImageUtils.getExtension(filename));
            mediaReader.addListener(isListener);
            while (!isListener.isImageGrabbed()) {
                try {
                    mediaReader.readPacket();
                } catch (Exception e) {
                    log.error("Exception: ", e);
                }
            }
        } catch (Exception e) {
            log.error("Exception: ", e);
        }

    }

    private static class ImageSnapListener extends MediaListenerAdapter {
        public boolean imageGrabbed = false;
        private static String uploadDir;
        private static String basename;
        private static String extension;
        public ImageSnapListener(String uploadDir, String basename, String extension) {
            this.uploadDir = uploadDir;
            this.basename = basename;
            this.extension = extension;
        }

        public void onVideoPicture(IVideoPictureEvent event) {
            if (event.getStreamIndex() != mVideoStreamIndex) {
                if (mVideoStreamIndex == -1) mVideoStreamIndex = event.getStreamIndex();
                else return;
            }
            if (mLastPtsWrite == Global.NO_PTS) mLastPtsWrite = event.getTimeStamp() - MICRO_SECONDS_BETWEEN_FRAMES;
            if (event.getTimeStamp() - mLastPtsWrite >= MICRO_SECONDS_BETWEEN_FRAMES) {
                BufferedImage bufferedImage = event.getImage();

                try {
                    BufferedImage image = ImageUtils.resize(event.getImage(), 200);
                    String thumbnailPath = uploadDir + "thumbs" + File.separator + basename+ "." + extension;
                    File output = new File(thumbnailPath);
                    ImageIO.write(image, "jpg", output);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.imageGrabbed = true; //set this var to true once an image is grabbed out of the movie.
                mLastPtsWrite += MICRO_SECONDS_BETWEEN_FRAMES;
            }
        }


        public boolean isImageGrabbed() {
            return imageGrabbed;
        }
    }
}
