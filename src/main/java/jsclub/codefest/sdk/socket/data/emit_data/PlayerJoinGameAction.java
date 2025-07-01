package jsclub.codefest.sdk.socket.data.emit_data;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class PlayerJoinGameAction {
    @SerializedName("joinCode")
    private String gameID;

    public PlayerJoinGameAction(String gameID) {
        this.gameID = gameID;
    }
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
