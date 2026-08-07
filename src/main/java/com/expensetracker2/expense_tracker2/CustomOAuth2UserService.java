package com.expensetracker2.expense_tracker2;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    // Only these Google accounts are allowed to log in
    private static final Set<String> ALLOWED_EMAILS = Set.of(
        "pruthvirao83@gmail.com"
    );

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);

        String email = user.getAttribute("email");

        if (email == null || !ALLOWED_EMAILS.contains(email.toLowerCase())) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("unauthorized_user"),
                "This account is not authorized to access this application."
            );
        }

        return user;
    }
}