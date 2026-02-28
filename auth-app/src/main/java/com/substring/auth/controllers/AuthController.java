package com.substring.auth.controllers;

import com.substring.auth.dtos.LoginRequest;
import com.substring.auth.dtos.TokenResponse;
import com.substring.auth.dtos.UserDto;
import com.substring.auth.entities.User;
import com.substring.auth.repositories.UserRepository;
import com.substring.auth.security.JwtService;
import com.substring.auth.services.AuthService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;

    private  final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private  final ModelMapper mapper;


    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @RequestBody LoginRequest loginRequest
    ){

        //authenticate
       Authentication authentication = authenticate(loginRequest);
        User user = userRepository.findByEmail(loginRequest.email()).orElseThrow(()-> new BadCredentialsException("Invalid Username or Password"));


        if(!user.isEnable()){
            throw  new DisabledException("User is disable");
        }

        //generate token

       String accessToken = jwtService.generateAccessToken(user);
       TokenResponse tokenResponse = TokenResponse.of(accessToken, "",jwtService.getAccessTtlSeconds(), mapper.map(user, UserDto.class));
        return  ResponseEntity.ok(tokenResponse);
    }


    private Authentication authenticate(LoginRequest loginRequest) {
        try {

         return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));

        }catch (Exception e){
            throw  new BadCredentialsException("Invalid Username or Password !");

        }
    }


    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@RequestBody UserDto userDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(userDto));

    }
}
