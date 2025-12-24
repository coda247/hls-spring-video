package com.joejoe2.video.controller.video;

import java.io.File;
import java.io.FileInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.joejoe2.video.service.storage.ObjectStorageService;

@Service
public class ThumbnailService {

    @Autowired
    private ObjectStorageService objectStorageService;

    public void extractAndUpload(
            MultipartFile videoFile,
            String userId,
            String videoFileName
    ) {

        File tempVideo = null;
        File thumbnail = null;

        try {
            // 1️⃣ Save uploaded video to temp file
            tempVideo = File.createTempFile("video-", ".mp4");
            videoFile.transferTo(tempVideo);

            // 2️⃣ Create temp thumbnail file
            thumbnail = File.createTempFile("thumb-", ".jpg");

            // 3️⃣ Extract thumbnail with FFmpeg
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-ss", "00:00:03",
                "-i", tempVideo.getAbsolutePath(),
                "-frames:v", "1",
                "-vf", "scale=640:-1",
                "-q:v", "2",
                thumbnail.getAbsolutePath()
            );

            Process process = pb.start();
            process.waitFor();

            // 4️⃣ Convert thumbnail File → MultipartFile
            MultipartFile thumbnailMultipart =
                new MockMultipartFile(
                    "file",
                    videoFileName + ".jpg",
                    "image/jpeg",
                    new FileInputStream(thumbnail)
                );

            // 5️⃣ Upload to object storage
            String objectName =
                "user/" + userId + "/thumbnails/" + videoFileName + ".jpg";

            objectStorageService.upload(thumbnailMultipart, objectName);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (tempVideo != null) tempVideo.delete();
            if (thumbnail != null) thumbnail.delete();
        }
    }
}
