package main.java.com.imagecloud.main.repository;

import com.imagecloud.main.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByUserIdOrderByUploadedAtDesc(String userId);
}
