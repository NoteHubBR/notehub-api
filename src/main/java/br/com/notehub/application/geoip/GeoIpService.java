package br.com.notehub.application.geoip;

import br.com.notehub.domain.token.Token;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import org.springframework.stereotype.Service;
import ua_parser.Client;
import ua_parser.Parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

@Service
public class GeoIpService {

    private final Parser uaParser;
    private final DatabaseReader geoReader;

    public GeoIpService() throws IOException {
        this.uaParser = new Parser();
        InputStream stream = getClass()
                .getClassLoader()
                .getResourceAsStream("geoip/GeoLite2-City.mmdb");
        this.geoReader = new DatabaseReader.Builder(stream).build();
    }

    private boolean isMobile(String ua) {
        String lower = ua.toLowerCase();
        return lower.contains("mobile") || lower.contains("android");
    }

    private void parseUserAgent(Token token, String userAgent) {
        Client client = uaParser.parse(userAgent);
        String browserVersion = client.userAgent.major != null ? " " + client.userAgent.major : "";
        String osVersion = client.os.major != null ? " " + client.os.major : "";
        token.setBrowser(client.userAgent.family + browserVersion);
        token.setOs(client.os.family + osVersion);
        if (client.device.family.equals("Other")) {
            token.setDeviceType("Desktop");
            token.setDeviceBrand("Unknown");
            token.setDeviceModel("Unknown");
        } else {
            token.setDeviceType(isMobile(userAgent) ? "Mobile" : "Tablet");
            token.setDeviceBrand(client.device.family);
            token.setDeviceModel(client.device.family);
        }
    }

    private void parseLocation(Token token, String ip) {
        try {
            if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) {
                token.setIp("local");
                token.setCountry("unknown");
                token.setRegion("unknown");
                token.setCity("unknown");
                return;
            }
            InetAddress address = InetAddress.getByName(ip);
            CityResponse response = geoReader.city(address);
            token.setCountry(response.country().name());
            token.setRegion(response.mostSpecificSubdivision().name());
            token.setCity(response.city().name());
        } catch (Exception e) {
            token.setCountry("unknown");
            token.setRegion("unknown");
            token.setCity("unknown");
        }
    }

    public void enrichToken(Token token, String userAgent, String ip) {
        parseUserAgent(token, userAgent);
        parseLocation(token, ip);
    }

}