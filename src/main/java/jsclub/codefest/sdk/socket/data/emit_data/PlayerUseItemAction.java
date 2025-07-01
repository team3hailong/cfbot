package jsclub.codefest.sdk.socket.data.emit_data;

import com.google.gson.annotations.SerializedName;

public class PlayerUseItemAction {
    @SerializedName("itemId")
    private String itemId;

    public PlayerUseItemAction(String itemId) {
        this.itemId = itemId;
    }
}
