package com.masterboxer.springbootmultitenancy.record;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank
        String tenant,
        @NotBlank
        String username,
        @NotBlank
        String password) {

    @Override
    public String toString() {
        return "LoginRequest[tenant=" + tenant + ", username=" + username + "]"; // never print the password
    }
}
