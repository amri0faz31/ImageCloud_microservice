package main.java.com.imagecloud.main.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversionResponse implements Serializable {
    private Long imageId;
    private byte[] convertedImageData;
    private boolean success;
    private String errorMessage;
}
