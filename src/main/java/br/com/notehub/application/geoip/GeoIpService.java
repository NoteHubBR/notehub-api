package br.com.notehub.application.geoip;

import br.com.notehub.domain.token.Token;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

@Service
public class GeoIpService {

    private final UserAgentAnalyzer uaAnalyzer;
    private final DatabaseReader geoReader;

    public GeoIpService() throws IOException {
        this.uaAnalyzer = UserAgentAnalyzer.newBuilder()
                .hideMatcherLoadStats()
                .withCache(1000)
                .build();
        InputStream stream = getClass()
                .getClassLoader()
                .getResourceAsStream("geoip/GeoLite2-City.mmdb");
        this.geoReader = new DatabaseReader.Builder(stream).build();
    }

    private String normalizeDeviceClass(String deviceClass) {
        if (deviceClass == null) return "unknown";
        return switch (deviceClass.toLowerCase()) {
            case "phone", "mobile" -> "Mobile";
            case "tablet" -> "Tablet";
            default -> "Desktop";
        };
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank() || value.equals("??") || value.equalsIgnoreCase("unknown")) return "unknown";
        return value;
    }

    private String normalizeOs(String osName) {
        if (osName == null) return "unknown";
        String os = osName.toLowerCase();
        if (os.contains("windows")) return "Windows";
        if (os.contains("mac") || os.contains("os x")) return "macOS";
        if (os.contains("android")) return "Android";
        if (os.contains("ios") || os.contains("iphone") || os.contains("ipad")) return "iOS";
        if (os.contains("linux")) return "Linux";
        if (os.contains("chrome os")) return "ChromeOS";
        return osName;
    }

    private void parseUserAgent(Token token, String userAgent) {
        UserAgent agent = uaAnalyzer.parse(userAgent);
        String browser = agent.getValue(UserAgent.AGENT_NAME);
        String browserVersion = agent.getValue(UserAgent.AGENT_VERSION_MAJOR);
        token.setBrowser(browser + (browserVersion != null ? " " + browserVersion : ""));
        token.setOs(normalizeOs(agent.getValue(UserAgent.OPERATING_SYSTEM_NAME)));
        String deviceClass = agent.getValue(UserAgent.DEVICE_CLASS);
        String brand = agent.getValue(UserAgent.DEVICE_BRAND);
        String model = agent.getValue(UserAgent.DEVICE_NAME);
        token.setDeviceBrand(sanitize(brand));
        token.setDeviceModel(sanitize(model));
        token.setDeviceType(normalizeDeviceClass(deviceClass));
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