package me.fivepb.ticketclip;

import com.velocitypowered.api.proxy.LoginPhaseConnection;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ResponderChain implements LoginPhaseConnection.MessageConsumer{
    private final int idx;
    private final LoginPhaseConnection con;
    private final LoginPluginDialog dialog;
    public ResponderChain(LoginPluginDialog dialog, int idx, LoginPhaseConnection con) {
        this.dialog = dialog;
        this.idx = idx;
        this.con = con;
    }


    @Override
    public void onMessageResponse(byte @Nullable [] responseBody) {
        int idxnew = idx + 1;
        if (dialog.getDialog().length < idxnew) {
            ResponderChain next = new ResponderChain(dialog, idxnew, con);
            LoginPluginDialog.DialogResponse dialogResponse = dialog.getDialog()[idxnew];
            con.sendLoginPluginMessage(dialogResponse.getChannel(), dialogResponse.getQuestion(), next);
        }

    }
}
