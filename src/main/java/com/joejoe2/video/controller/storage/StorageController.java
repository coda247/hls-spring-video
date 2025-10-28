package com.joejoe2.video.controller.storage;

import com.joejoe2.video.config.ObjectStorageConfiguration;
import com.joejoe2.video.controller.constraint.auth.AuthenticatedApi;
import com.joejoe2.video.data.UserDetail;
import com.joejoe2.video.data.storage.UploadRequest;
import com.joejoe2.video.service.storage.ObjectStorageService;
import com.joejoe2.video.utils.AuthUtil;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.io.InputStream;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping(path = "/api/storage") // path prefix
public class StorageController {
  @Autowired ObjectStorageService objectStorageService;
  @Autowired ObjectStorageConfiguration objectStorageConfiguration;

  @Autowired
  private MinioClient minioClient;
  //@AuthenticatedApi
//   @SecurityRequirement(name = "jwt")
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
    } catch (Exception e) {
        
      throw new RuntimeException(e);
    }
    return ResponseEntity.ok().build();
  }

    //@AuthenticatedApi
//   @SecurityRequirement(name = "jwt")
  @Operation(description = "upload video file")
  @ApiResponses
  @RequestMapping(
      path = "/image/upload",
      method = RequestMethod.POST,
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Map<String, Object>>  image(@Valid UploadRequest request) {
    //UserDetail user = AuthUtil.currentUserDetail();
    MultipartFile file = request.getFile();
    String objectName = request.getFileName();
    try {
      // upload
       String fileUrl = objectStorageService.image(file, objectName);
        Map<String, Object> response = new HashMap<>();
         response.put("url", fileUrl);
     return ResponseEntity.ok(response);
    } catch (Exception e) {
         e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
            

    }

  }


  

   @RequestMapping(
    path ="/{filename:.+}",
    method = RequestMethod.GET
   )
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
  
  /*@AuthenticatedApi
  @RequestMapping(path = "/download", method = RequestMethod.GET)
  public void download(@ParameterObject @Valid DownloadRequest request,
                                 HttpServletRequest servletRequest,
                                 HttpServletResponse servletResponse) {
      UserDetail user = AuthUtil.currentUserDetail();
      String objectName = "user/"+user.getId()+"/"+request.getFileName();
      try {
          objectStorageService.download(objectName, request.getFileName(), servletRequest, servletResponse);
      } catch (DoesNotExist e) {
          servletResponse.setStatus(404);
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
  }*/

  /*@AuthenticatedApi
  @RequestMapping(path = "/file", method = RequestMethod.GET)
  public ResponseEntity getFile(@ParameterObject @Valid FileRequest request) {

  }*/

  /*
  @AuthenticatedApi
  @RequestMapping(path = "/file", method = RequestMethod.DELETE)
  public ResponseEntity deleteFile(@ParameterObject @Valid FileRequest request) {
      UserDetail user = AuthUtil.currentUserDetail();
      try {
          fileService.delete(user.getId(), request.getPath(), false);
      }catch (DoesNotExist e){
          return ResponseEntity.notFound().build();
      }
      return ResponseEntity.ok().build();
  }*/

  /*@AuthenticatedApi
  @RequestMapping(path = "/folder", method = RequestMethod.POST)
  public ResponseEntity createFolder(@RequestBody @Valid FolderRequest request) {
      UserDetail user = AuthUtil.currentUserDetail();
      String folderName = request.getPath().substring(request.getPath().lastIndexOf("/")+1);
      try {
          fileService.createFolder(user.getId(), request.getPath(), folderName);
      }catch (InvalidOperation e) {
          throw new RuntimeException(e);
      }
      return ResponseEntity.ok().build();
  }*/

  /*@AuthenticatedApi
  @RequestMapping(path = "/folder", method = RequestMethod.GET)
  public ResponseEntity getFolder(@ParameterObject @Valid FolderRequest request) {
      try {
          return ResponseEntity.ok(new FileResponse(
                  fileService.get(AuthUtil.currentUserDetail().getId(), request.getPath(), true)
          ));
      }catch (DoesNotExist e){
          return ResponseEntity.notFound().build();
      }
  }*/

  /*@AuthenticatedApi
  @RequestMapping(path = "/folder", method = RequestMethod.DELETE)
  public ResponseEntity deleteFolder(@ParameterObject @Valid FolderRequest request) {
      UserDetail user = AuthUtil.currentUserDetail();
      try {
          fileService.delete(user.getId(), request.getPath(), true);
      }catch (DoesNotExist e){
          return ResponseEntity.notFound().build();
      }
      return ResponseEntity.ok().build();
  }*/
}
