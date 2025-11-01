package com.example.financebackend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest.Builder;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom OAuth2AuthorizationRequestResolver to add prompt=select_account parameter
 * This forces Google to always show account selection screen instead of auto-login
 */
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;

    public CustomOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository, 
                                                    String authorizationRequestBaseUri) {
        this.defaultResolver = new org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository, authorizationRequestBaseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
        return customizeAuthorizationRequest(authorizationRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request, clientRegistrationId);
        return customizeAuthorizationRequest(authorizationRequest);
    }

    private OAuth2AuthorizationRequest customizeAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest) {
        if (authorizationRequest == null) {
            return null;
        }

        // Add prompt and access_type parameters to improve OAuth2 flow
        // prompt=select_account: Always show account selection screen
        // access_type=online: Use online access (no refresh token needed for basic login)
        // This ensures session is preserved through the OAuth2 redirect flow
        Map<String, Object> additionalParameters = new HashMap<>(authorizationRequest.getAdditionalParameters());
        additionalParameters.put("prompt", "select_account");
        additionalParameters.put("access_type", "online");
        
        // Add include_granted_scopes=true to ensure proper token handling
        additionalParameters.put("include_granted_scopes", "true");

        Builder builder = OAuth2AuthorizationRequest.from(authorizationRequest)
            .additionalParameters(additionalParameters);

        return builder.build();
    }
}

