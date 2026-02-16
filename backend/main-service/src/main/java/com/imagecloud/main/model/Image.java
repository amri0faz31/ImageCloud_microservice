package main.java.com.imagecloud.main.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String originalFormat;

    @Column(nullable = false)
    private String targetFormat;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ConversionStatus status;

    @Lob
    @Column(columnDefinition = "bytea")
    private byte[] originalImage;

    @Lob
    @Column(columnDefinition = "bytea")
    private byte[] convertedImage;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column
    private LocalDateTime convertedAt;

    @Column
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
