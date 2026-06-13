package com.backendauth.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
   private String accessToken;
   private String refreshToken;

   @Builder.Default
   private String tokenType = "Bearer";

}

