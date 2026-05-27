package com.cityguard.auth.security;

import com.cityguard.auth.entity.LoginUser;
import com.cityguard.auth.mapper.SysUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final SysUserMapper sysUserMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
            String username = jwtUtils.getUsernameFromToken(token);
            Long userId = jwtUtils.getUserIdFromToken(token);

            LoginUser loginUser = new LoginUser();
            loginUser.setId(userId);
            loginUser.setUsername(username);

            List<String> roleCodes = new ArrayList<>();
            try {
                List<String> fromDb = sysUserMapper.selectRoleCodesByUserId(userId);
                if (fromDb != null) {
                    roleCodes.addAll(fromDb);
                }
            } catch (Exception ex) {
                // 不因角色加载失败阻断请求；无角色时方法级鉴权会拒绝敏感接口
            }
            loginUser.setRoles(roleCodes);

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            for (String role : roleCodes) {
                if (StringUtils.hasText(role)) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.trim()));
                }
            }

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
