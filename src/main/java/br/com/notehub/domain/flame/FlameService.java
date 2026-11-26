package br.com.notehub.domain.flame;

import br.com.notehub.application.dto.response.flame.DetailFlameRES;
import br.com.notehub.application.dto.response.page.PageRES;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface FlameService {

    DetailFlameRES inflame(UUID userIdFromToken, UUID noteIdFromPath);

    void deflame(UUID userIdFromToken, UUID noteIdFromPath);

    PageRES<DetailFlameRES> getAllUserFlamesById(Pageable pageable, UUID idFromToken);

    PageRES<DetailFlameRES> searchUserFlamesBySpecs(Pageable pageable, UUID idFromToken, String username, String q);

}