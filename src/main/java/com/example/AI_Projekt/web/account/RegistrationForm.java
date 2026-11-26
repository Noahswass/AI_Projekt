package com.example.AI_Projekt.web.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** What the registration form sends. The limits match the app_user table and BCrypt. */
@Getter
@Setter
public class RegistrationForm {

    @NotBlank(message = "Choose a username.")
    @Size(min = 3, max = 40, message = "The username must be 3 to 40 characters.")
    @Pattern(regexp = "[A-Za-z0-9._-]*", message = "Use only letters, digits, dot, dash and underscore.")
    private String username;

    // BCrypt ignores everything after 72 bytes, so longer passwords are refused instead of cut
    @NotBlank(message = "Choose a password.")
    @Size(min = 8, max = 72, message = "The password must be 8 to 72 characters.")
    private String password;

    @NotBlank(message = "Type the password once more.")
    private String confirmPassword;
}
