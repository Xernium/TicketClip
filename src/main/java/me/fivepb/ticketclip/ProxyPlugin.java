package me.fivepb.ticketclip;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(
        id = "ticketclip",
        name = "TicketClip",
        version = BuildConstants.VERSION,
        description = "Small Velocity plugin that allows replaying of saved login plugin message handshakes",
        authors = {"FivePB"}
)
public class ProxyPlugin {

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer proxy;

    public Logger getLogger() {
        return logger;
    }

    @Inject
    @DataDirectory
    private Path pluginDataDir;

    @MonotonicNonNull
    private Map<String, LoginPluginDialog> mappings = null;

    private ConcurrentHashMap<InboundConnection, LoginPluginDialog> trackedPlayers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Player, LoginPluginDialog> trackecPlayersPost = new ConcurrentHashMap<Player, LoginPluginDialog>();


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        ensureCreated(pluginDataDir.toFile());
        ImmutableMap.Builder<String, LoginPluginDialog> builder = ImmutableMap.builder();
        Path global = subFolder(pluginDataDir, "global");
        LoginPluginDialog apex = loadFromFiles(global);
        if (apex != null) {
            builder.put("", apex);
        }

        Path fh = subFolder(pluginDataDir, "forcedhosts");
        File[] fhfolders = fh.toFile().listFiles();
        for (File f : fhfolders) {
            if(f.isDirectory()) {
                String name = f.getName();
                LoginPluginDialog dialog = loadFromFiles(f.toPath());
                if (dialog != null) {
                    builder.put(name, dialog);
                }
            }
        }

        mappings = builder.build();

        /*// Register channels:
        for(LoginPluginDialog pds : mappings.values()) {
            for(LoginPluginDialog.DialogResponse pdr : pds.getDialog()) {

            }
        }
        */

        proxy.getEventManager().register(this, new ClientListener(this));

    }

    @Nullable
    public LoginPluginDialog trackConnection(InboundConnection conn, String host) {
        if (conn instanceof Player) {
            Player p = (Player) conn;
            if (trackecPlayersPost.containsKey(p)) {
                return trackecPlayersPost.get(p);
            } else {
                for(InboundConnection ic : trackedPlayers.keySet()) {
                    if (ic.getRemoteAddress() == p.getRemoteAddress()) {
                        LoginPluginDialog f = trackedPlayers.get(ic);
                        trackedPlayers.remove(ic);
                        trackecPlayersPost.put(p, f);
                        return f;
                    }
                }
                return null;
            }
        } else {

            if (trackedPlayers.containsKey(conn)) {
                return trackedPlayers.get(conn);
            }

            LoginPluginDialog d = mappings.get(host);
            if (d == null) {
                return null;
            }
            trackedPlayers.put(conn, d);

            return d;
        }
    }

    public void untrackConnection(InboundConnection conn) {
        trackedPlayers.remove(conn);
        if (conn instanceof Player) {
            trackecPlayersPost.remove((Player) conn);
        }
    }

    @Nullable
    private static LoginPluginDialog loadFromFiles(Path directory) {
        ensureCreated(directory.toFile());
        File[] files = directory.toFile().listFiles();
        LoginPluginDialog ret = null;
        for(File f : files) {
            if(f.getName().endsWith(".json")) {
                if(ret != null) {
                    throw new IllegalArgumentException("Too many responses for " + directory.toFile().getName());
                }
                try {
                    String content = Files.readString(f.toPath());
                    JsonArray jsa = JsonParser.parseString(content).getAsJsonArray();
                    ret = new LoginPluginDialog(jsa);
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
        return ret;
    }

    private static Path subFolder(Path p, String name) {
        File ret = new File(p.toString(), name);
        ensureCreated(ret);
        return ret.toPath();
    }

    private static void ensureCreated(File f) {
        if (!f.exists()) {
            Preconditions.checkArgument(f.mkdirs(), "Unable to create directory " + f.getName());
        }
    }
}
