package com.substring.auth.config;

public class AppConstants {

    /**
     * Public endpoints that should be accessible without authentication.
     *
     * Keep this list minimal; most APIs should remain protected.
     */
    public static final String[] AUTH_PUBLIC_URLS = {
            // Register is intentionally the only public auth API
            "/api/v1/auth/register",
            "/api/v1/auth/login",

            // Needed so framework error dispatches don't trigger auth loops
            "/error",

            // API docs (consider disabling in prod via springdoc.* properties)
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**"
    };
    public   static  final String ADMIN_ROLE="ADMIN";
    public   static  final String GUEST_ROLE="GUEST";

}
