package com.nqind.authhack;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.GameProfile;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AuthMainHook {
    private final ProxyServer server;
    private final Logger logger;
    private final Cache<InboundConnection, String> playerCache = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(20, TimeUnit.SECONDS)
            .build();

    @Inject
    public AuthMainHook(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;

        logger.info("h");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // TODO
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        InboundConnection con = event.getConnection();
        String user = event.getUsername();

        // TODO Test if username is in database
        if(false) {
            logger.info("Redirecting authentication for username " + user);
            // Set to offline and add to player cache
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
            playerCache.put(con, user);
        }
    }

    @Subscribe
    public void onGameProfileRequest(GameProfileRequestEvent event) {
        String user = playerCache.getIfPresent(event.getConnection());

        // Is the user in the cache?
        if(user != null) {
            GameProfile currProfile = event.getGameProfile();
            event.setGameProfile(new GameProfile(
                    UUID.fromString(user), // TODO Better UUID generation
                    user,
                    Collections.emptyList()
            ));
        }
    }
}
