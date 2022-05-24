package me.fivepb.ticketclip;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent;
import com.velocitypowered.api.proxy.LoginPhaseConnection;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class ClientListener {

    private final ProxyPlugin plugin;

    public ClientListener(ProxyPlugin plugin) {
        this.plugin = plugin;
    }


    @Subscribe
    public void onPreLogin(PreLoginEvent e) {
        LoginPhaseConnection con = (LoginPhaseConnection) e.getConnection();
        LoginPluginDialog d = plugin.trackConnection(con, con.getVirtualHost().isPresent() ? con.getVirtualHost().get().getHostString() : "");
        if (d != null && d.getDialog().length > 0) {
            ResponderChain rc = new ResponderChain(d, 0, con);
            LoginPluginDialog.DialogResponse di = d.getDialog()[0];
            con.sendLoginPluginMessage(di.getChannel(), di.getQuestion(), rc);
        }
    }

    @Subscribe
    public void onServerLoginPlugin(ServerLoginPluginMessageEvent e) {
        LoginPluginDialog d = plugin.trackConnection(e.getConnection().getPlayer(), "ignored");
        if (d != null) {
            ChannelIdentifier channelIdentifier = e.getIdentifier();
            byte[] data = e.getContents();

            for(int i = 0; i < d.getDialog().length; i++) {
                LoginPluginDialog.DialogResponse cur = d.getDialog()[i];
                if (channelIdentifier.equals(cur.getChannel()) && Arrays.equals(data, cur.getQuestion())) {
                    e.setResult(ServerLoginPluginMessageEvent.ResponseResult.reply(cur.getResponse()));
                    return;
                }
            }
            plugin.getLogger().warn("Don't know how to answer " + e.getIdentifier().toString());

        }
    }


}
