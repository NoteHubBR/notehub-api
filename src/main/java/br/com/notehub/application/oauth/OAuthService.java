package br.com.notehub.application.oauth;

import br.com.notehub.application.dto.oauth.OAuthResponse;
import org.springframework.stereotype.Service;

@Service
public interface OAuthService {

    OAuthResponse getUser(String key);

}