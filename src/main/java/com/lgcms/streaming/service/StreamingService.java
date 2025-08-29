package com.lgcms.streaming.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.lgcms.streaming.common.dto.exception.BaseException;
import com.lgcms.streaming.common.dto.exception.StreamingError;
import com.lgcms.streaming.common.kafka.dto.EncodingStatus;
import com.lgcms.streaming.common.kafka.dto.LectureEncodeDto;
import com.lgcms.streaming.common.kafka.dto.EncodingSuccess;
import com.lgcms.streaming.config.FfmpegConfig;
import com.lgcms.streaming.event.producer.EncodingEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class StreamingService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    @Value("${cloud.aws.cdn.url}")
    private String prefix;

    private final AmazonS3Client amazonS3Client;
    private final FfmpegConfig ffmpegConfig;
    private final EncodingEventService encodingEventService;


    public void downloadOriginFile(LectureEncodeDto lectureEncodeDto) {
        String originKey = lectureEncodeDto.getKey();
        String lectureId = lectureEncodeDto.getLectureId();
        String lessonId = lectureEncodeDto.getLessonId();
        File originFile = null;
        Path outputDir = null;

        try {
            //s3 원본 저장
            originFile = File.createTempFile("origin-", ".tmp");
            amazonS3Client.getObject(new GetObjectRequest(bucket, originKey), originFile);
            log.info("S3 다운로드 완료: {}", originKey);

            outputDir = Files.createTempDirectory("lecture-" + lectureId);
            Path outputDir720 = outputDir.resolve("720p");
            Files.createDirectories(outputDir720);
            Path outputDir1080 = outputDir.resolve("1080p");
            Files.createDirectories(outputDir1080);

            FFmpeg ffmpeg = new FFmpeg(ffmpegConfig.getFfmpegPath());
            FFprobe ffprobe = new FFprobe(ffmpegConfig.getFfprobePath());
            double duration = ffprobe.probe(originFile.getAbsolutePath())
                    .getFormat()
                    .duration;

            String targetPrefix = lectureId + "/" + lessonId + "/" + "hls/";
            generateHLS(ffmpeg, ffprobe, originFile.toPath(), outputDir720,
                    "720p", "1280", "720", 2_500_000);
            generateHLS(ffmpeg, ffprobe, originFile.toPath(), outputDir1080,
                    "1080p", "1920", "1080", 5_000_000);
            generateThumbnail(ffmpeg, ffprobe, originFile.toPath(), outputDir, "thumbnail.jpg", "0");

            String masterM3U8 = "#EXTM3U\n"
                    + "#EXT-X-VERSION:3\n\n"
                    + "#EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720\n"
                    + "720p/720p.m3u8\n"
                    + "#EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080\n"
                    + "1080p/1080p.m3u8\n";
            Files.writeString(outputDir.resolve("master.m3u8"), masterM3U8);

            //s3 업로드
            try (Stream<Path> paths = Files.walk(outputDir)) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    String key = targetPrefix + outputDir.relativize(path).toString().replace("\\", "/");
                    amazonS3Client.putObject(bucket, key, path.toFile());
                }
                //원본 삭제
                amazonS3Client.deleteObject(bucket, originKey);
                //이벤트
                System.out.println(prefix + targetPrefix + "master.m3u8");
                System.out.println(prefix + targetPrefix + "thumbnail.jpg");
                EncodingSuccess videoEncodingSuccess = EncodingSuccess.builder()
                        .duration((int) duration)
                        .status("성공")
                        .lessonId(lectureEncodeDto.getLessonId())
                        .lectureId(lectureId)
                        .memberId(lectureEncodeDto.getMemberId())
                        .videoUrl(prefix + targetPrefix + "master.m3u8")
                        .thumbnailUrl(prefix + targetPrefix + "thumbnail.jpg")
                        .build();
                encodingEventService.EncodingSuccessEvent(videoEncodingSuccess);
            } catch (Exception e) {
                log.error("S3 업로드 실패", e);
                String[] parts = lectureId.split("_", 2); // limit=2 로 나누기
                String lectureName = parts[1];

                EncodingStatus encodingStatus = EncodingStatus.builder()
                        .status("실패")
                        .lectureId(lectureId)
                        .memberId(lectureEncodeDto.getMemberId())
                        .lectureName(lectureName)
                        .build();
                encodingEventService.EncodingFailEvent(encodingStatus);
                throw new BaseException(StreamingError.ENCODING_ERROR);
            }

        } catch (Exception e) {
            log.error("upload fail : {}", originKey, e);
            deleteDirectoryRecursively(outputDir);
            throw new BaseException(StreamingError.ENCODING_ERROR);
        } finally {
            Optional.ofNullable(originFile).ifPresent(File::delete);
            Optional.ofNullable(outputDir).ifPresent(this::deleteDirectoryRecursively);
        }
    }

    private void generateHLS(FFmpeg ffmpeg, FFprobe ffprobe, Path input, Path outputDir,
                             String label, String width, String height, int bitrate) {
        try {
            String m3u8Name = label + ".m3u8";
            String segmentPattern = label + "_%03d.ts";

            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(input.toString())
                    .overrideOutputFiles(true)
                    .addOutput(outputDir.resolve(m3u8Name).toString())
                    .setFormat("hls")
                    .setVideoCodec("libx264")
                    .setAudioCodec("aac")
                    .setAudioBitRate(128_000)
                    .setVideoBitRate(bitrate)
                    .addExtraArgs("-vf", "scale=" + width + ":" + height)
                    .addExtraArgs("-hls_time", "6")
                    .addExtraArgs("-hls_list_size", "0")
                    .addExtraArgs("-hls_segment_filename", outputDir.resolve(segmentPattern).toString())
                    .addExtraArgs("-crf", "23")
                    .addExtraArgs("-preset", "veryfast")
                    .done();

            new FFmpegExecutor(ffmpeg, ffprobe).createJob(builder).run();
        } catch (Exception e) {
            log.error("hsl generate fail : {}", label, e);
        }
    }

    public void generateThumbnail(FFmpeg ffmpeg, FFprobe ffprobe, Path input, Path outputDir, String fileName, String timestamp) {
        try {
            String thumbnailPath = outputDir.resolve(fileName).toString();

            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(input.toString())
                    .overrideOutputFiles(true)
                    .addOutput(thumbnailPath)
                    .setFormat("image2")
                    .setFrames(1)
                    .addExtraArgs("-ss", String.valueOf(timestamp))
                    .done();

            new FFmpegExecutor(ffmpeg, ffprobe).createJob(builder).run();
        } catch (Exception e) {
            log.error("thumbnail generate fail", e);
        }
    }

    private void deleteDirectoryRecursively(Path dir) {
        if (dir != null && Files.exists(dir)) {
            try {
                Files.walk(dir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                log.warn("임시 디렉토리 삭제 실패: {}", dir, e);
            }
        }
    }
}
