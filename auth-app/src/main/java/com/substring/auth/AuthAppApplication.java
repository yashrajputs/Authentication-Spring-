package com.substring.auth;

import com.substring.auth.config.AppConstants;
import com.substring.auth.entities.Role;
import com.substring.auth.repositories.RoleRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.UUID;

@SpringBootApplication
public class AuthAppApplication implements CommandLineRunner {

	@Autowired
	private RoleRepository roleRepository;
	public static void main(String[] args) {
		SpringApplication.run(AuthAppApplication.class, args);

	}
	@Override
	public  void run(String @NonNull ... args) throws  Exception{
		roleRepository.findByName("ROLE_"+AppConstants.ADMIN_ROLE).ifPresentOrElse(role->{
			System.out.println("Admin Role Already Exists: "+role.getName());
		},()->{
			Role role=new Role();
			role.setName("ROLE_"+AppConstants.ADMIN_ROLE);
			role.setId(UUID.randomUUID());
			roleRepository.save(role);
		});
		roleRepository.findByName("ROLE_"+AppConstants.GUEST_ROLE).ifPresentOrElse(role->{

			System.out.println("Guest Role Already Exists: "+role.getName());
		},()->{
			Role role=new Role();
			role.setName("ROLE_"+AppConstants.GUEST_ROLE);
			role.setId(UUID.randomUUID());
			roleRepository.save(role);
		});
	}

}
