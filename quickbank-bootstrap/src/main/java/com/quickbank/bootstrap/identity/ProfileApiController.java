package com.quickbank.bootstrap.identity;

import com.quickbank.account.app.AccountApplicationService;
import com.quickbank.bootstrap.identity.IdentityDirectory.StoredProfile;
import com.quickbank.common.security.ClientUser;
import com.quickbank.common.security.ClientUsers;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileApiController {

    private final IdentityService identity;
    private final AccountApplicationService accounts;

    public ProfileApiController(IdentityService identity, AccountApplicationService accounts) {
        this.identity = identity;
        this.accounts = accounts;
    }

    @GetMapping
    @Operation(summary = "Read the signed-in client's profile")
    public ProfileResponse me() {
        return ProfileResponse.from(identity.profile(ClientUsers.require().subject()));
    }

    @PutMapping
    @Operation(summary = "Update the signed-in client's name and email. Email must stay unique.")
    public ProfileResponse update(@Valid @RequestBody ProfileRequest request) {
        ClientUser user = ClientUsers.require();
        StoredProfile updated = identity.updateProfile(user.subject(), request.email(), request.firstName(), request.lastName());
        accounts.updateOwnerContact(user.subject(), updated.firstName() + " " + updated.lastName(), updated.email());
        return ProfileResponse.from(updated);
    }

    public record ProfileRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName
    ) {
    }

    public record ProfileResponse(String username, String email, String firstName, String lastName) {
        static ProfileResponse from(StoredProfile profile) {
            return new ProfileResponse(profile.username(), profile.email(), profile.firstName(), profile.lastName());
        }
    }
}
