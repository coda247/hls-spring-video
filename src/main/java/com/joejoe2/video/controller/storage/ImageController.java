import java.io.InputStream;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.joejoe2.video.config.ObjectStorageConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;

@Controller
@RequestMapping(path = "/images")
public class ImageController {
  @Autowired ObjectStorageConfiguration objectStorageConfiguration;

    @Autowired
    private MinioClient minioClient;



    @GetMapping("/{filename:.+}") // allow dots in filename
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            
            if (filename.contains("..") || filename.contains("/")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
            }
   String BUCKET = objectStorageConfiguration.getImageBucket();
            // Fetch the file from MinIO
            InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(BUCKET)
                    .object(filename)
                    .build()
            );

            // Guess MIME type from filename
            String mimeType = URLConnection.guessContentTypeFromName(filename);
            if (mimeType == null) {
                mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

           return ResponseEntity.ok()
    .contentType(MediaType.parseMediaType(mimeType))
    .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
    .body(new InputStreamResource(stream));

        } catch (MinioException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + filename, e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading file", e);
        }
    }
}
