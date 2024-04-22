package com.example.steam.controller;

import com.example.steam.dto.User;
import com.example.steam.config.JwtTokenProvider;
import com.example.steam.entity.OAuthTokens;
import com.example.steam.model.GoogleUser;
import com.example.steam.model.NaverUser;
import com.example.steam.service.GoogleOAuthService;
import com.example.steam.service.NaverOAuthService;
import com.example.steam.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/oauth")
public class OAuthController {

    @Autowired
    private GoogleOAuthService googleOAuthService;

    @Autowired
    private NaverOAuthService naverOAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static final Logger logger = LoggerFactory.getLogger(OAuthController.class);

    @GetMapping("/google/login")
    public String googleLogin() {
        return googleOAuthService.getGoogleAuthorizationUrl();
    }

    @GetMapping("/google/callback")
    public void googleCallback(@RequestParam("code") String code, HttpServletResponse response) {
        logger.info("Google OAuth 콜백 처리 시작: code={}", code);
        try {
            OAuthTokens tokens = googleOAuthService.getAccessToken(code);
            GoogleUser googleUser = googleOAuthService.getUserInfo(tokens.getAccessToken());

            User user = userService.processGoogleUser(googleUser, tokens.getAccessToken());
            String jwt = jwtTokenProvider.generateToken(new UsernamePasswordAuthenticationToken(user.getUsername(), null, Collections.emptyList())).getAccessToken();
            String redirectUrlWithToken = "https://localhost:3000/?token=" + jwt;
            response.sendRedirect(redirectUrlWithToken);
        } catch (Exception e) {
            logger.error("Google OAuth 콜백 처리 중 오류 발생", e);
            try {
                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Google OAuth 처리 중 오류 발생");
            } catch (IOException ioException) {
                logger.error("리다이렉트 실패", ioException);
            }
        }
    }

    @GetMapping("/naver/login")
    public String naverLogin() {
        return naverOAuthService.getNaverAuthorizationUrl();
    }

    @GetMapping("/naver/callback")
    public void naverCallback(@RequestParam("code") String code, HttpServletResponse response) {
        try {
            OAuthTokens tokens = naverOAuthService.getAccessToken(code);
            NaverUser naverUser = naverOAuthService.getUserInfo(tokens.getAccessToken());

            User user = userService.processNaverUser(naverUser, tokens.getAccessToken());
            String jwt = jwtTokenProvider.generateToken(new UsernamePasswordAuthenticationToken(user.getUsername(), null, Collections.emptyList())).getAccessToken();
            String redirectUrlWithToken = "https://localhost:3000/?token=" + jwt;
            response.sendRedirect(redirectUrlWithToken);
        } catch (Exception e) {

        }
    }

}
