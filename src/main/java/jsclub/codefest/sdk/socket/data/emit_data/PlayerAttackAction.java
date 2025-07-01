package jsclub.codefest.sdk.socket.data.emit_data;

import com.google.gson.annotations.SerializedName;

public class PlayerAttackAction {
    @SerializedName("direction")
    private String direction;

    public PlayerAttackAction(String direction) {
        this.direction = direction;
    }
}