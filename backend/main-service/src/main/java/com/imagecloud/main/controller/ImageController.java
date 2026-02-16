package main.java.com.imagecloud.main.controller;

import com.imagecloud.main.dto.ImageHistoryResponse;
import com.imagecloud.main.dto.ImageUploadResponse;
import com.imagecloud.main.model.Image;
import com.imagecloud.main.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/upload")
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetFormat") String targetFormat,
            @RequestHeader("X-User-Id") String userId) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ImageUploadResponse(null, "ERROR", "File is empty"));
        }

        ImageUploadResponse response = imageService.uploadAndConvert(file, targetFormat, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ImageHistoryResponse>> getHistory(
            @RequestHeader("X-User-Id") String userId) {

        List<ImageHistoryResponse> history = imageService.getUserHistory(userId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{imageId}/status")
    public ResponseEntity<Image> getImageStatus(
            @PathVariable Long imageId,
            @RequestHeader("X-User-Id") String userId) {

        Image image = imageService.getImageStatus(imageId, userId);
        return ResponseEntity.ok(image);
    }

    @GetMapping("/{imageId}/download")
    public ResponseEntity<byte[]> downloadConvertedImage(
            @PathVariable Long imageId,
            @RequestHeader("X-User-Id") String userId) {

        try {
            byte[] imageData = imageService.getConvertedImage(imageId, userId);
            Image image = imageService.getImageStatus(imageId, userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String filename = image.getOriginalFileName().replaceFirst("[.][^.]+$", "") 
                    + "." + image.getTargetFormat();
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(imageData, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}
