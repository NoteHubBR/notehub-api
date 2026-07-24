package br.com.notehub.application.media;

import br.com.notehub.application.dto.request.media.UploadAvatarREQ;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static br.com.notehub.infra.exception.CustomExceptions.MediaProcessingException;

@Service
@RequiredArgsConstructor
public class TranscodingAvatarService {

    private static final long MAX_GIF_SIZE = 12 * 1024 * 1024;
    private static final int MAX_DIMENSION = 256;

    private final RestClient restClient;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    private void validate(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("Arquivo vazio");
        if (file.getSize() > MAX_GIF_SIZE) throw new IllegalArgumentException("GIF excede 12MB");
        if (!"image/gif".equals(file.getContentType())) throw new IllegalArgumentException("Arquivo não é um GIF");
    }

    private void runFfmpeg(Path input, Path output) throws IOException, InterruptedException {
        List<String> command = List.of(
                "ffmpeg",
                "-y",
                "-i", input.toString(),
                "-vf",
                "scale=w='min(%d,iw)':h='min(%d,ih)':force_original_aspect_ratio=decrease".formatted(MAX_DIMENSION, MAX_DIMENSION),
                "-c:v", "libvpx-vp9",
                "-pix_fmt", "yuva420p",
                "-b:v", "0",
                "-crf", "32",
                "-an",
                output.toString()
        );
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String ffmpegOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new MediaProcessingException("FFmpeg excedeu o timeout");
        }
        if (process.exitValue() != 0) {
            throw new MediaProcessingException("FFmpeg falhou (exit=%d): %s".formatted(process.exitValue(), ffmpegOutput));
        }
    }

    private void uploadToSupabase(Path file, String bucket, String path) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        String uri = "%s/storage/v1/object/%s/%s".formatted(supabaseUrl, bucket, path);
        restClient.put()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                .header("apikey", serviceRoleKey)
                .contentType(MediaType.valueOf("video/webm"))
                .body(bytes)
                .retrieve()
                .toBodilessEntity();
    }

    private void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best-effort cleanup de arquivo temporário
        }
    }

    public String transcodeAndUpload(UploadAvatarREQ request) {
        validate(request.file());
        Path input = null;
        Path output = null;
        try {
            input = Files.createTempFile("avatar-in-", ".gif");
            request.file().transferTo(input);
            output = Files.createTempFile("avatar-out-", ".webm");
            runFfmpeg(input, output);
            String path = "%s/%s/%s.webm".formatted(request.folder().name().toLowerCase(), request.username(), UUID.randomUUID());
            String bucket = request.bucket();
            uploadToSupabase(output, bucket, path);
            return "%s/storage/v1/object/public/%s/%s".formatted(supabaseUrl, bucket, path);
        } catch (IOException | InterruptedException e) {
            throw new MediaProcessingException("Falha ao processar o avatar em GIF", e);
        } finally {
            deleteQuietly(input);
            deleteQuietly(output);
        }
    }

}