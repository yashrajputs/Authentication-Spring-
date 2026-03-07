package com.substring.auth.services.impl;

import com.substring.auth.config.AppConstants;
import com.substring.auth.dtos.UserDto;
import com.substring.auth.entities.Provider;
import com.substring.auth.entities.Role;
import com.substring.auth.entities.User;
import com.substring.auth.exceptions.ResourceNotFoundException;
import com.substring.auth.helpers.UserHelper;
import com.substring.auth.repositories.RoleRepository;
import com.substring.auth.repositories.UserRepository;
import com.substring.auth.services.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.HashSet;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {


    private  final UserRepository userRepository;
    private  final ModelMapper modelMapper;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        if(userDto.getEmail()== null || userDto.getEmail().isBlank()){
            throw new IllegalArgumentException("Email is required");
        }if(userRepository.existsByEmail(userDto.getEmail())){
            throw new IllegalArgumentException("Email already exists");
        }

        User user = modelMapper.map(userDto, User.class);

        // Ensure roles collection is never null to prevent NPEs when adding default role
        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }

        // Ensure new users are enabled by default after registration.
        // If the client didn't send an explicit enable flag, force true.
        if (userDto.getEnable() == null) {
            user.setEnable(true);
        } else {
            user.setEnable(userDto.getEnable());
        }

        user.setProvider(userDto.getProvider()!=null ? userDto.getProvider() : Provider.LOCAL);

        Role role = roleRepository.findByName("ROLE_"+ AppConstants.GUEST_ROLE).orElse(null);
        user.getRoles().add(role);


        User savedUser = userRepository.save(user);
        return modelMapper.map(savedUser, UserDto.class);
    }

    @Override
    public UserDto getUserByEmail(String email) {

       User user = userRepository
               .findByEmail(email)
               .orElseThrow(()-> new ResourceNotFoundException("User not found with given email id"));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    public UserDto updateUser(UserDto userDto, String userId) {
        UUID uId = UserHelper.parseUUID(userId);
        User existingUser = userRepository
                .findById(uId)
                .orElseThrow(()-> new ResourceNotFoundException("User not found with given id"));
        if(userDto.getName()!= null) existingUser.setName(userDto.getName());
        if(userDto.getImage()!= null) existingUser.setImage(userDto.getImage());
        if(userDto.getProvider()!= null) existingUser.setProvider(userDto.getProvider());
        //TODO: CHANGE
        if (userDto.getPassword()!= null) existingUser.setPassword(userDto.getPassword());
        if (userDto.getEnable() != null) {
            existingUser.setEnable(userDto.getEnable());
        }
        existingUser.setUpdatedAt(Instant.now());
        User updatedUser =userRepository.save(existingUser);
        return modelMapper.map(updatedUser,UserDto.class);
    }

    @Override
    public void deleteUser(String userId) {
       UUID uId = UserHelper.parseUUID(userId);

       User user = userRepository.findById(uId)
               .orElseThrow(()-> new ResourceNotFoundException("User not found"));
       userRepository.delete(user);

    }

    @Override
    public UserDto getUserById(String userId) {

     User user = userRepository.findById(UserHelper.parseUUID(userId)).orElseThrow(()-> new ResourceNotFoundException("User not found with given id"));

        return modelMapper.map(user, UserDto.class);
    }

    @Override
    @Transactional
    public Iterable<UserDto> getAllUsers() {
        return userRepository
                .findAll()
                .stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();

    }
}
