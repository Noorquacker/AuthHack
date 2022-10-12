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

import java.net.URI;
import java.net.http.HttpClient;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class AuthMainHook {
    private final ProxyServer server;
    private final Logger logger;
    private final HttpClient client;
    private final Cache<InboundConnection, ArrayList<String>> playerCache = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(20, TimeUnit.SECONDS)
            .build();

    @Inject
    public AuthMainHook(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        this.client = HttpClient.newHttpClient();

        logger.info("h");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // TODO
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {

        // Some setup crap
        InboundConnection con = event.getConnection();
        String user = event.getUsername();
        boolean in_db = false;
        String uuid = null;


        // Building the request for the database
        var request = HttpRequest.newBuilder(
                        URI.create("https://mc.nqind.com/repo/authhack/api.php"))
                .POST(BodyPublishers.ofString("{\"client_id\": \"local-client\"}"))
                .build();

        // Testing if it's in the database
        try {

            // Parsing the response
            var response = client.send(request, HttpResponse.BodyHandlers.ofString()); // TODO TODO TODO TODO
            JSONObject j = new JSONObject(response.body());
            List<Object> users = ((JSONArray) j.get("users")).toList();

            // Looping over database
            for (Object i : users) {
                ArrayList<Object> k = (ArrayList) i;
                if (k.get(0).equals(user)) {
                    uuid = (String) k.get(1);
                    // Check if IP is good!
                    String con_ip = con.getRemoteAddress().getAddress().toString().substring(1);
                    in_db = con_ip.equals(k.get(2));
                    logger.debug("Connecting from " + con_ip + ", this is " + (in_db ? "ALLOWED" : "DENIED"));
                    break;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse response! " + e + e.getMessage() + e.getCause() + e.getStackTrace().toString());
        }

        ArrayList<String> user_uuid = new ArrayList<String>();
        user_uuid.add(user);
        user_uuid.add(uuid);


        if (in_db) {
            logger.debug("Redirecting authentication for username " + user);
            // Set to offline and add to player cache
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
            playerCache.put(con, user_uuid);
        }
    }

    @Subscribe
    public void onGameProfileRequest(GameProfileRequestEvent event) {
        ArrayList<String> user_uuid = playerCache.getIfPresent(event.getConnection());

        // Is the user in the cache?
        if (user_uuid != null) {
            event.setGameProfile(new GameProfile(
                    UUID.fromString(user_uuid.get(1)),
                    user_uuid.get(0),
                    Collections.emptyList()
            ));
        }
    }
}
