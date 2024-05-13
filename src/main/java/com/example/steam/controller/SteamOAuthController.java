package com.example.steam.controller;

import com.example.steam.config.JwtTokenProvider;
import com.example.steam.dto.CustomUserDetails;
import com.example.steam.dto.User;
import com.example.steam.service.SteamAuthenticationService;
import com.example.steam.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RequestMapping("/oauth/steam")
@RestController
public class SteamOAuthController {
    private static final Logger logger = LoggerFactory.getLogger(SteamOAuthController.class);

    private final UserService userService;
    private final SteamAuthenticationService steamService;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public SteamOAuthController(UserService userService, SteamAuthenticationService steamService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.steamService = steamService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Autowired
    private SteamAuthenticationService steamAuthService;

    @GetMapping("/login")
    public ResponseEntity<?> getSteamLoginUrl() {
        String redirectUrl = "https://localhost:3000/oauth/steam/callback"; // 필요에 따라 조정
        try {
            String loginUrl = steamAuthService.buildSteamLoginUrl(redirectUrl);
            return ResponseEntity.ok(Map.of("steamLoginUrl", loginUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Steam 로그인 URL 생성 중 오류 발생");
        }
    }


    @GetMapping("/connect")
    public ResponseEntity<?> getSteamConnectUrl() {
        try {
            String redirectUrl = "https://localhost:8080/oauth/steam/callback";
            String loginUrl = steamAuthService.buildSteamLoginUrl(redirectUrl);
            return ResponseEntity.ok(Map.of("url", loginUrl));
        } catch (Exception e) {
            logger.error("Steam 로그인 URL 생성 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Steam 로그인 URL 생성 중 오류 발생");
        }
    }

    @GetMapping("/callback")
    public ResponseEntity<?> handleSteamCallback(@RequestParam("openid.assoc_handle") String assocHandle,
                                                 @RequestParam("openid.claimed_id") String claimedId,
                                                 @RequestParam("openid.sig") String signature) {
        try {
            // 스팀 사용자 처리 로직
            steamAuthService.processSteamUser(claimedId, "SteamUser"); // 스팀 사용자 정보 처리. 스팀에서 정보가져오는거 처리 필요함

            // 인증 객체 확인
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("스팀 인증 실패");
            }

            // JWT 토큰 생성
            String token = jwtTokenProvider.generateToken(authentication).getAccessToken();
            return ResponseEntity.ok(Map.of("accessToken", token, "claimedId", claimedId, "assocHandle", assocHandle));

        } catch (Exception e) {
            logger.error("스팀 인증 실패d", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("스팀 인증 실패");
        }
    }
}