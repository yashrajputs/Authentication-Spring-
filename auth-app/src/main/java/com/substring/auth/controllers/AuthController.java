package com.substring.auth.controllers;

import com.substring.auth.dtos.LoginRequest;
import com.substring.auth.dtos.RefreshTokenRequest;
import com.substring.auth.dtos.TokenResponse;
import com.substring.auth.dtos.UserDto;
import com.substring.auth.entities.RefreshToken;
import com.substring.auth.entities.User;
import com.substring.auth.repositories.RefreshTokenRepository;
import com.substring.auth.repositories.UserRepository;
import com.substring.auth.security.CookieService;
import com.substring.auth.security.JwtService;
import com.substring.auth.services.AuthService;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenRepository refreshTokenRepository;
    private  final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private  final ModelMapper mapper;
    private final CookieService cookieService;


    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @RequestBody LoginRequest loginRequest, HttpServletResponse response
            ){

        //authenticate
       Authentication authentication = authenticate(loginRequest);
        User user = userRepository.findByEmail(loginRequest.email()).orElseThrow(()-> new BadCredentialsException("Invalid Username or Password"));


        if(!user.isEnable()){
            throw  new DisabledException("User is disable");
        }


        String jti = UUID.randomUUID().toString();
        var refreshTokenOb= RefreshToken.builder()
                .jti(jti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenOb);

        //access generate token

       String accessToken = jwtService.generateAccessToken(user);
       String refreshToken = jwtService.generateRefreshToken(user, refreshTokenOb.getJti());

       cookieService.attachRefreshCookie(response, refreshToken, (int)jwtService.getRefreshTtlSeconds());
       cookieService.addNoStoreHeaders(response);
        TokenResponse tokenResponse = TokenResponse.of(accessToken,refreshToken, jwtService.getAccessTtlSeconds(), mapper.map(user, UserDto.class));
        return  ResponseEntity.ok(tokenResponse);
    }


    private Authentication authenticate(LoginRequest loginRequest) {
        try {
//uska provider google ya github ya local nhi hai us ko bata na hai ki  thing apne kisi social mieda use kr rkha hai login krne ke liye ap usko use kro
         return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));

        }catch (Exception e){
            throw  new BadCredentialsException("Invalid Username or Password !");

        }
    }
    // access and refresh api
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse>refreshToken(
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletResponse response,
            HttpServletRequest request

    ){

        String refreshToken = readRefreshTokenFromRequest(body, request).orElseThrow(()-> new BadCredentialsException("RefreshToken is missing"));

        if(!jwtService.isRefreshToken(refreshToken)){
            throw new BadCredentialsException("Invalid Refresh Token Type");
        }
         String jti = jwtService.getJti(refreshToken);
        UUID userId = jwtService.getUserId(refreshToken);
      RefreshToken storedRefreshToken =  refreshTokenRepository.findByJti(jti).orElseThrow(()-> new BadCredentialsException("Refresh token not recognized"));
      if(storedRefreshToken.isRevoked()){
          throw new BadCredentialsException("Refresh token expired or revoked");
      }
      if(storedRefreshToken.getExpiresAt().isBefore(Instant.now())){
          throw new BadCredentialsException("Refresh token expired");
      }
      if (!storedRefreshToken.getUser().getId().equals(userId)){
          throw new BadCredentialsException("Refresh token does not belong t this user");
      }

      storedRefreshToken.setRevoked(true);
      String newJti = UUID.randomUUID().toString();
      storedRefreshToken.setReplacedByToken(newJti);
      refreshTokenRepository.save(storedRefreshToken);

      User user = storedRefreshToken.getUser();

        var newRefreshTokenOb= RefreshToken.builder()
                .jti(newJti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();

refreshTokenRepository.save(newRefreshTokenOb);
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user, newRefreshTokenOb.getJti());

        cookieService.attachRefreshCookie(response, newRefreshToken, (int)jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeaders(response);
        TokenResponse tokenResponse = TokenResponse.of(newAccessToken,newRefreshToken, jwtService.getAccessTtlSeconds(), mapper.map(user, UserDto.class));
        return  ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response){
        readRefreshTokenFromRequest( null, request).ifPresent(token
                -> {
            try {
                if (jwtService.isRefreshToken(token)) {
                    String jti = jwtService.getJti(token);
                    refreshTokenRepository.findByJti(jti).ifPresent(rt ->  {
                        rt.setRevoked(true);
                        refreshTokenRepository.save(rt);
                    });
                }
            } catch (JwtException ignored) {

            }

        });

        cookieService.clearRefreshCookie(response);
        cookieService.addNoStoreHeaders(response);
        SecurityContextHolder.clearContext();
        return  ResponseEntity.status(HttpStatus.NO_CONTENT).build();
   }


    private Optional<String> readRefreshTokenFromRequest(RefreshTokenRequest body, HttpServletRequest request) {
        if (request.getCookies() != null) {
            Optional<String> fromCookie = Arrays.stream(
                            request.getCookies()
                    ).filter(c -> cookieService.getRefreshTokenCookieName().equals(c.getName()))
                    .map(Cookie::getValue)
                    .filter(v -> !v.isBlank())
                    .findFirst();
            if (fromCookie.isPresent()) {
                return fromCookie;
            }
        }
        if(body!=null && body.refreshToken()!=null && !body.refreshToken().isBlank()){
            return  Optional.of(body.refreshToken());
        }
       String refreshHeader = request.getHeader("X-Refresh-Token");
        if(refreshHeader !=null && !refreshHeader.isBlank()) {
            return  Optional.of(refreshHeader.trim());
        }
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if(authHeader != null && authHeader.regionMatches(true, 0, "Bearer",0,6 )) {
            String candidate = authHeader.substring(7).trim();
            if (!candidate.isEmpty()) {
                try {
                    if (jwtService.isRefreshToken(candidate)) {
                        return Optional.of(candidate);
                    }
                } catch (Exception ignored) {

                }
            }
        }
        return Optional.empty();

    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@RequestBody UserDto userDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(userDto));

    }
}
