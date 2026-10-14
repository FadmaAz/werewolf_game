package com.example.werewolf.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * Determines the STOMP Principal from the HTTP session so that
 * {@code convertAndSendToUser(...)} can target a specific user.
 *
 * The session attribute {@code USER_ID} (set by the login controllers)
 * is used as the principal name.
 */
public class HttpSessionHandshakeHandler extends DefaultHandshakeHandler {

  public static final String USER_ID_ATTR = "USER_ID";

  @Override
  protected Principal determineUser(ServerHttpRequest request,
                                    WebSocketHandler wsHandler,
                                    Map<String, Object> attributes) {
    if (request instanceof ServletServerHttpRequest servletRequest) {
      HttpSession session = servletRequest.getServletRequest().getSession(false);
      if (session != null) {
        Object userId = session.getAttribute(USER_ID_ATTR);
        if (userId != null) {
          return new StompPrincipal(String.valueOf(userId));
        }
      }
    }
    return null;
  }

  public static class StompPrincipal implements Principal {
    private final String name;

    public StompPrincipal(String name) { this.name = name; }

    @Override
    public String getName() { return name; }
  }
}
