package com.photobuddy.security;

import com.photobuddy.entity.User;
import com.photobuddy.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        String identifier = usernameOrEmail.trim().toLowerCase(Locale.ROOT);
        User user = users.findByEmail(identifier).or(() -> users.findByUsername(identifier)).orElseThrow(
                () -> new UsernameNotFoundException("User not found"));
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())), user.isEnabled());
    }
}
