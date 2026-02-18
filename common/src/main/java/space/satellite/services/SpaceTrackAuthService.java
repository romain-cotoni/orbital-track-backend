package space.satellite.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import space.satellite.config.SpaceTrackProperties;
import space.satellite.exceptions.LoginException;

import static space.satellite.constants.Constants.SPACETRACK_BASE_URL;
import static space.satellite.constants.Constants.SPACETRACK_LOGIN_URL;

/**
 * Shared authentication service for the Space-Track API.
 * Centralizes login logic so all modules can use the same
 * authentication flow: proper form encoding, HTTP status validation, and session cookie extraction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpaceTrackAuthService {

    private static final String LOGIN_URL = SPACETRACK_BASE_URL + SPACETRACK_LOGIN_URL;

    private final SpaceTrackProperties spaceTrackProperties;

    /**
     * Authenticates with Space-Track and returns the session cookie.
     * The provided RestClient is used to perform the request.
     * If the client has a cookie jar configured (e.g. the batch), the session cookie is captured automatically for subsequent requests.
     * Callers that manage cookies manually (e.g. the API module) can use the returned cookie string directly.
     */
    public String login(RestClient restClient) {
        log.info("Logging in to Space-Track...");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("identity", spaceTrackProperties.getIdentity());
        formData.add("password", spaceTrackProperties.getPassword());

        ResponseEntity<String> response = restClient.post()
                .uri(LOGIN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .toEntity(String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new LoginException("SpaceTrack login failed with status: " + response.getStatusCode());
        }

        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        if (!StringUtils.hasText(cookie)) {
            log.info("No new session cookie received — existing session still active");
            return null;
        }

        log.info("Successfully logged in to Space-Track");
        return cookie;
    }
}
