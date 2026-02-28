package com.substring.auth.dtos;


import jakarta.persistence.Column;
import lombok.*;

import java.util.UUID;

@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleDto {
    private UUID id;
    private  String name;
}
