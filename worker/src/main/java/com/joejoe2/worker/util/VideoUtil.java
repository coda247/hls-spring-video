package com.joejoe2.worker.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoUtil {
  private static final Logger logger = LoggerFactory.getLogger(VideoUtil.class);

  public static void transcodeToM3u8(File source, File workDir) throws IOException {
    List<String> command = mp4ToHlsCommand(source.getAbsolutePath());
    Process process = new ProcessBuilder().command(command).directory(workDir).start();

    new Thread(
            () -> {
              try (BufferedReader bufferedReader =
                  new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                  logger.info(line);
                }
              } catch (IOException e) {
              }
            })
        .start();

    new Thread(
            () -> {
              try (BufferedReader bufferedReader =
                  new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                  logger.info(line);
                }
              } catch (IOException e) {
              }
            })
        .start();

    try {
      if (process.waitFor() != 0) {
        throw new RuntimeException("exit with " + process.exitValue());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static List<String> mp4ToThumbnail(String src) {
    List<String> cmd = new ArrayList<>();

    cmd.add("ffmpeg");
    cmd.add("-y");

    cmd.add("-ss");
    cmd.add("00:00:03"); // seek to 3s (avoid black frames)

    cmd.add("-i");
    cmd.add(src);

    cmd.add("-frames:v");
    cmd.add("1");

    cmd.add("-vf");
    cmd.add("scale=640:-1");

    cmd.add("-q:v");
    cmd.add("2"); // high quality

    cmd.add("thumbnail.jpg");

    return cmd;
}

  // private static List<String> mp4ToHlsCommand(String src) {
  //   List<String> command = new ArrayList<>();
  //   command.add("ffmpeg");

  //   command.add("-i");
  //   command.add(src);

  //   // Use ultrafast preset — lowest CPU load
  //   command.add("-c:v");
  //   command.add("libx264");
  //   command.add("-preset");
  //   command.add("ultrafast");
  //   command.add("-crf");
  //   command.add("28"); // Lower quality, faster encoding

  //   // Downscale if needed (e.g., from 1080p to 720p)
  //   command.add("-vf");
  //   command.add("scale=-2:720"); // Keeps aspect ratio, scales height to 720

  //   // Audio (lightweight settings)
  //   command.add("-c:a");
  //   command.add("aac");
  //   command.add("-b:a");
  //   command.add("96k"); // Lower audio bitrate
  //   command.add("-ac");
  //   command.add("1");   // Mono audio to reduce processing

  //   // HLS output
  //   command.add("-f");
  //   command.add("hls");
  //   command.add("-hls_time");
  //   command.add("4");
  //   command.add("-hls_list_size");
  //   command.add("0");
  //   command.add("-hls_segment_filename");
  //   command.add("segment_%03d.ts");
  //   command.add("-start_number");
  //   command.add("0");

  //   command.add("index.m3u8");


  //   return command;
  // }

  private static List<String> mp4ToHlsCommand(String src) {
    List<String> cmd = new ArrayList<>();

    cmd.add("ffmpeg");
    cmd.add("-y");

    cmd.add("-i");
    cmd.add(src);

    // Video encoding (enterprise safe)
    cmd.add("-c:v");
    cmd.add("libx264");
    cmd.add("-preset");
    cmd.add("ultrafast");
    cmd.add("-tune");
    cmd.add("zerolatency");

    // Keyframes aligned to HLS segments
    cmd.add("-g");
    cmd.add("96");              // 4s * 24fps
    cmd.add("-keyint_min");
    cmd.add("96");
    cmd.add("-sc_threshold");
    cmd.add("0");

    // Bitrate control (VERY IMPORTANT)
    cmd.add("-b:v");
    cmd.add("1800k");
    cmd.add("-maxrate");
    cmd.add("2000k");
    cmd.add("-bufsize");
    cmd.add("3600k");

    // Scaling
    cmd.add("-vf");
    cmd.add("scale=-2:720");

    // Audio
    cmd.add("-c:a");
    cmd.add("aac");
    cmd.add("-b:a");
    cmd.add("96k");
    cmd.add("-ac");
    cmd.add("1");

    // HLS
    cmd.add("-f");
    cmd.add("hls");
    cmd.add("-hls_time");
    cmd.add("4");
    cmd.add("-hls_list_size");
    cmd.add("0");
    cmd.add("-hls_flags");
    cmd.add("independent_segments");

    cmd.add("-hls_segment_filename");
    cmd.add("segment_%05d.ts");

    cmd.add("index.m3u8");

    return cmd;
}


}