package br.com.notehub.application.dto.request.media;

import br.com.notehub.application.media.MediaFolder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record UploadAvatarREQ(
        @NotNull MultipartFile file,
        @NotBlank String bucket,
        @NotBlank String username,
        @NotNull MediaFolder folder
) {
}