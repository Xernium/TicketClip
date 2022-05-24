package me.fivepb.ticketclip;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.util.*;

public class LoginPluginDialog {

    private final DialogResponse[] dialog;

    public LoginPluginDialog(JsonArray jsa) {
        final int len = jsa.size();
        Map<Integer, DialogResponse> dlr = new HashMap<Integer, DialogResponse>();

        for(int i = 0; i < len; i++) {
            JsonObject jso = (JsonObject) jsa.get(i);
            byte[] question = Base64.getDecoder().decode(jso.get("question").getAsString());
            byte[] response = Base64.getDecoder().decode(jso.get("answer").getAsString());
            MinecraftChannelIdentifier identifier = MinecraftChannelIdentifier.from(jso.get("identifier").getAsString());
            int idx = jso.get("index").getAsInt();
            Preconditions.checkArgument(dlr.put(idx, new DialogResponse(question, response, identifier)) == null, "Duplicate entry");
        }
        dialog = new DialogResponse[len];
        for(int i = 0; i < len; i++) {
            DialogResponse d = dlr.get(i);
            Preconditions.checkNotNull(d, "No element for " + i);
            dialog[i] = d;
        }
    }

    public DialogResponse[] getDialog() {
        return dialog;
    }

    public class DialogResponse{
        private final byte[] question;
        private final byte[] response;
        private final MinecraftChannelIdentifier channel;

        private DialogResponse(byte[] question, byte[] response, MinecraftChannelIdentifier channel) {
            this.question = question;
            this.response = response;
            this.channel = channel;
        }

        public byte[] getQuestion() {
            return question;
        }

        public byte[] getResponse() {
            return response;
        }

        public MinecraftChannelIdentifier getChannel() {
            return channel;
        }
    }
}
