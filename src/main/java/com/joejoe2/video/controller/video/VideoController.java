package com.joejoe2.video.controller.video;

import com.joejoe2.video.controller.constraint.auth.AuthenticatedApi;
import com.joejoe2.video.data.UserDetail;
import com.joejoe2.video.data.storage.UploadRequest;
import com.joejoe2.video.data.video.CreateRequest;
import com.joejoe2.video.data.video.TsRequest;
import com.joejoe2.video.data.video.VideoProfile;
import com.joejoe2.video.data.video.VideoRequest;
import com.joejoe2.video.exception.DoesNotExist;
import com.joejoe2.video.models.video.VideoStatus;
import com.joejoe2.video.service.storage.ObjectStorageService;
import com.joejoe2.video.service.video.VideoService;
import com.joejoe2.video.utils.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.Map;
import java.util.UUID;
import javax.validation.Valid;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Controller
@RequestMapping(path = "/api/video") // path prefix
public class VideoController {
  @Autowired VideoService videoService;
  @Autowired ObjectStorageService objectStorageService;

  private VideoStatus waitUntilReady(UUID videoId, long timeoutMs) throws InterruptedException {
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < timeoutMs) {
        try {
            VideoProfile profile = videoService.profile(videoId);
            VideoStatus status = profile.getStatus();

            if (status == VideoStatus.READY) return VideoStatus.READY;
            if (status == VideoStatus.ERROR) return VideoStatus.ERROR;
        } catch (Exception ignored) {}

        Thread.sleep(3000);
    }
    return VideoStatus.PROCESSING;
}


  @Operation(description = "upload video file")
  @ApiResponses
  @RequestMapping(
      path = "/upload",
      method = RequestMethod.POST,
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity upload(@Valid UploadRequest request) {
    //UserDetail user = AuthUtil.currentUserDetail();
    MultipartFile file = request.getFile();
    String userId = "1b659551-ee80-4acc-8ea4-ade098fea4a5";
    String objectName = "user/" + userId + "/" + request.getFileName();
    try {
      // upload
      objectStorageService.upload(file, objectName);
       VideoProfile profile = videoService.createFromObjectStorage(
                UUID.fromString(userId), userId, objectName);

        UUID videoId = profile.getId();

     VideoStatus status = waitUntilReady(videoId, 180_000);
System.out.println("FIGO " + status);

switch (status) {
    case READY -> {
        String hlsUrl = "/video/" + videoId + "/index.m3u8";
        return ResponseEntity.ok(Map.of(
                "status", status.toString(),
                "videoId", videoId.toString(),
                "hlsUrl", hlsUrl
        ));
    }

    case ERROR -> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", status.toString(),
                        "videoId", videoId.toString()
                ));
    }

    default -> {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of(
                        "status", status.toString(),
                        "videoId", videoId.toString()
                ));
    }
}

    } catch (Exception e) {
        
      throw new RuntimeException(e);
    }
  }

  //@AuthenticatedApi
  // @SecurityRequirement(name = "jwt")
  @Operation(description = "create video from file")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "create video from file and start processing",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = VideoProfile.class))),
      })
  @RequestMapping(path = "/", method = RequestMethod.POST)
  public ResponseEntity create(@Valid @RequestBody CreateRequest request) {
    // UserDetail user = AuthUtil.currentUserDetail();
     String userId = "1b659551-ee80-4acc-8ea4-ade098fea4a5";
    String objectName = "user/" + userId + "/" + request.getFileName();


    VideoProfile profile =
        videoService.createFromObjectStorage(
            UUID.fromString(userId), request.getTitle(), objectName);
    return ResponseEntity.ok(profile);
  }



  @Operation(description = "get hls index file of the video")
  @RequestMapping(
      path = "/{id}/index.m3u8",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<StreamingResponseBody> index(@Valid @ParameterObject VideoRequest request) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set("Content-Type", "application/vnd.apple.mpegurl");
      headers.set("Content-Disposition", "attachment;filename=index.m3u8");
      StreamingResponseBody body = videoService.m3u8Index(UUID.fromString(request.getId()));
      return new ResponseEntity<>(body, headers, HttpStatus.OK);
    } catch (DoesNotExist e) {
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(description = "get hls index file of the video")
  @RequestMapping(
      path = "/{id}/{ts:.+}.ts",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<StreamingResponseBody> ts(@Valid @ParameterObject TsRequest request) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set("Content-Type", "application/vnd.apple.mpegurl");
      headers.set("Content-Disposition", "attachment;filename=" + request.getTs() + ".ts");
      StreamingResponseBody body =
          videoService.ts(UUID.fromString(request.getId()), request.getTs() + ".ts");
      return new ResponseEntity<>(body, headers, HttpStatus.OK);
    } catch (DoesNotExist e) {
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(description = "get video profile")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "get video profile",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = VideoProfile.class))),
        @ApiResponse(
            responseCode = "404",
            description = "target video does not exist",
            content = @Content),
      })
  @RequestMapping(path = "/{id}/profile", method = RequestMethod.GET)
  public ResponseEntity profile(@Valid @ParameterObject VideoRequest request) {
    try {
      VideoProfile profile = videoService.profile(UUID.fromString(request.getId()));
      return ResponseEntity.ok(profile);
    } catch (DoesNotExist e) {
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(description = "get video list")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "get a list of video profiles",
            content =
                @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = VideoProfile.class)))),
      })
  @RequestMapping(path = "/list", method = RequestMethod.GET)
  public ResponseEntity list() {
    return ResponseEntity.ok(videoService.list());
  }
}
