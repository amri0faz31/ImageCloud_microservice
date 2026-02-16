package main.java.com.imagecloud.conversion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversionRequest implements Serializable {
    private Long imageId;
    private byte[] imageData;
    private String originalFormat;
    private String targetFormat;
}
