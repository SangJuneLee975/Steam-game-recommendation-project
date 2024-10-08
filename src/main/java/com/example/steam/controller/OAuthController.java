package com.example.steam.controller;

import com.example.steam.dto.CustomUserDetails;
import com.example.steam.dto.User;
import com.example.steam.config.JwtTokenProvider;
import com.example.steam.entity.OAuthTokens;
import com.example.steam.entity.RefreshToken;
import com.example.steam.entity.SocialLogin;
import com.example.steam.model.GoogleUser;
import com.example.steam.model.NaverUser;
import com.example.steam.repository.SocialLoginRepository;
import com.example.steam.service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
    private SocialLoginRepository socialLoginRepository;

    @Autowired
    private NaverUserService naverUserService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenService refreshTokenService;

    private static final Logger logger = LoggerFactory.getLogger(OAuthController.class);

    // 구글 로그인 URL 반환
    @GetMapping("/google/login")
    public String googleLogin() {
        return googleOAuthService.getGoogleAuthorizationUrl();
    }

    // 구글 OAuth 콜백 처리
    @GetMapping("/google/callback")
    public void googleCallback(@RequestParam("code") String code, HttpServletResponse response) {
        logger.info("Google OAuth 콜백 처리 시작: code={}", code);
        try {
            OAuthTokens tokens = googleOAuthService.getAccessToken(code);
            GoogleUser googleUser = googleOAuthService.getUserInfo(tokens.getAccessToken());

            User user = userService.processGoogleUser(googleUser, tokens.getAccessToken());


            CustomUserDetails userDetails = new CustomUserDetails(
                    user.getUsername(),
                    user.getPassword(),
                    user.getName(),
                    user.getSocialCode(),
                    user.getSteamId(),
                    user.getAuthorities()
            );

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, // Principal as CustomUserDetails
                    null,        // Credentials
                    userDetails.getAuthorities()  // Authorities
            );

            String jwt = jwtTokenProvider.generateToken(authentication).getAccessToken();

            String redirectUrlWithToken = "https://stdash.shop/?token=" + jwt;
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
    public void naverCallback(@RequestParam("code") String code, @RequestParam("state") String state, HttpServletResponse response) {
        try {
            OAuthTokens tokens = naverOAuthService.getAccessToken(code, state);
            NaverUser naverUser = naverOAuthService.getUserInfo(tokens.getAccessToken());

            User user = naverUserService.processNaverUser(naverUser, tokens.getAccessToken());

            Optional<SocialLogin> socialLoginOpt = socialLoginRepository.findByUser(user);
            Integer socialCode = socialLoginOpt.isPresent() ? socialLoginOpt.get().getSocialCode() : null;  // socialCode가 없는 경우 null 사용


            CustomUserDetails userDetails = new CustomUserDetails(
                    user.getUsername(),
                    user.getPassword(),
                    user.getName(),
                    socialCode,
                    user.getSteamId(),
                    user.getAuthorities()
            );

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, // Principal as CustomUserDetails
                    null,        // Credentials
                    userDetails.getAuthorities()  // Authorities
            );

            String jwt = jwtTokenProvider.generateToken(authentication).getAccessToken();

            // 클라이언트에 전달할 토큰 정보
            String redirectUrlWithToken = "https://stdash.shop/?token=" + jwt;
            response.sendRedirect(redirectUrlWithToken);
        } catch (Exception e) {
            logger.error("Naver OAuth 콜백 처리 중 오류 발생", e);
            try {
                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Naver OAuth 처리 중 오류 발생");
            } catch (IOException ioException) {
                logger.error("리다이렉트 실패", ioException);
            }
        }
    }

    
}