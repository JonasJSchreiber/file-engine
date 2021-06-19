package com.jonasjschreiber.fileengine.utils;

import io.humble.video.*;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;
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
        if (filename == null || filename.length() == 0) return false;
        return VIDEO_TYPES.contains(ImageUtils.getExtension(filename.toLowerCase()));
    }

    public static final double SECONDS_BETWEEN_FRAMES = 1;
    private static int mVideoStreamIndex = -1;
    private static long mLastPtsWrite = Global.NO_PTS;
    public static final long MICRO_SECONDS_BETWEEN_FRAMES = (long) (Global.DEFAULT_PTS_PER_SECOND * SECONDS_BETWEEN_FRAMES);

    public static void generateVideoThumbnailHumble(String uploadDir, String filename) throws IOException, InterruptedException {
        Demuxer demuxer = Demuxer.make();
        demuxer.open(filename, null, false, true, null, null);
        int numStreams = demuxer.getNumStreams();
        int videoStreamId = -1;
        long streamStartTime = Global.NO_PTS;
        Decoder videoDecoder = null;
        for(int i = 0; i < numStreams; i++) {
            final DemuxerStream stream = demuxer.getStream(i);
            streamStartTime = stream.getStartTime();
            final Decoder decoder = stream.getDecoder();
            if (decoder != null && decoder.getCodecType() == MediaDescriptor.Type.MEDIA_VIDEO) {
                videoStreamId = i;
                videoDecoder = decoder;
                break;
            }
        }

        if (videoStreamId == -1) throw new RuntimeException("could not find video stream in container: " + filename);

        /*
         * Now we have found the audio stream in this file.  Let's open up our decoder so it can
         * do work.
         */
        videoDecoder.open(null, null);

        final MediaPicture picture = MediaPicture.make(
                videoDecoder.getWidth(),
                videoDecoder.getHeight(),
                videoDecoder.getPixelFormat());

        final MediaPictureConverter converter =
                MediaPictureConverterFactory.createConverter(
                        MediaPictureConverterFactory.HUMBLE_BGR_24,
                        picture);
        boolean generated = false;
        long systemStartTime = System.nanoTime();
        final Rational systemTimeBase = Rational.make(1, 1000000000);
        final Rational streamTimebase = videoDecoder.getTimeBase();
        final MediaPacket packet = MediaPacket.make();
        while(demuxer.read(packet) >= 0 && !generated) {
            if (packet.getStreamIndex() == videoStreamId) {
                /**
                 * A packet can actually contain multiple sets of samples (or frames of samples
                 * in decoding speak).  So, we may need to call decode  multiple
                 * times at different offsets in the packet's data.  We capture that here.
                 */
                int offset = 0;
                int bytesRead = 0;
                do {
                    bytesRead += videoDecoder.decode(picture, packet, offset);
                    if (picture.isComplete()) {
                        generateThumbnail(uploadDir, ImageUtils.getBaseNameOfFile(filename),
                                ImageUtils.getExtension(filename), streamStartTime,
                                picture, converter, systemStartTime, systemTimeBase, streamTimebase);
                        generated = true;
                        break;
                    }
                    offset += bytesRead;
                } while (!generated && offset < packet.getSize());
            }
        }
    }

    private static void generateThumbnail(String uploadDir, String basename, String extension, long streamStartTime,
                                          final MediaPicture picture, final MediaPictureConverter converter,
                                          long systemStartTime, final Rational systemTimeBase, final Rational streamTimebase)
            throws InterruptedException, IOException {
        long streamTimestamp = picture.getTimeStamp();
        streamTimestamp = systemTimeBase.rescale(streamTimestamp-streamStartTime, streamTimebase);
        long systemTimestamp = System.nanoTime();
        while (streamTimestamp > (systemTimestamp - systemStartTime + 1000000)) {
            Thread.sleep(1);
            systemTimestamp = System.nanoTime();
        }
        BufferedImage image = ImageUtils.resize(converter.toImage(null, picture), 200);
        String thumbnailPath = uploadDir + "thumbs" + File.separator + basename + "." + extension;
        File output = new File(thumbnailPath);
        ImageIO.write(image, "jpg", output);
    }
}
