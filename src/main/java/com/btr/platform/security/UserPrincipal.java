package com.btr.platform.security;

public record UserPrincipal(Long id, String account, String nickname, String role) {
}
