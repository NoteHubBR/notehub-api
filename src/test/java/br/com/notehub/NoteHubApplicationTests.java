package br.com.notehub;

import br.com.notehub.application.geoip.GeoIpService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NoteHubApplicationTests {

    @MockBean
    private GeoIpService geoIpService;

    @Test
    void contextLoads() {
    }

}