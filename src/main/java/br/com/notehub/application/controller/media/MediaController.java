package br.com.notehub.application.controller.media;

import br.com.notehub.application.dto.request.media.UploadAvatarREQ;
import br.com.notehub.application.dto.response.media.UploadAvatarRES;
import br.com.notehub.application.media.TranscodingAvatarService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/medias")
@Hidden
@RequiredArgsConstructor
public class MediaController {

    private final TranscodingAvatarService avatarService;

    @PostMapping(value = "/gif/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadAvatarRES> uploadAvatarGif(@ModelAttribute @Valid UploadAvatarREQ request) {
        String publicUrl = avatarService.transcodeAndUpload(request);
        return ResponseEntity.ok(new UploadAvatarRES(publicUrl));
    }

}