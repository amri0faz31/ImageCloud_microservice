package main.java.com.imagecloud.main.dto;

import com.imagecloud.main.model.ConversionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageHistoryResponse {
    private Long id;
    private String originalFileName;
    private String originalFormat;
    private String targetFormat;
    private ConversionStatus status;
    private LocalDateTime uploadedAt;
    private LocalDateTime convertedAt;
    private String errorMessage;
}
