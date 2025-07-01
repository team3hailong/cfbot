package jsclub.codefest.sdk.socket;

public class EventName {
    //Listening Event (Server -> SDK)
    public static final String ON_MAP_INIT = "game:state:init";
    public static final String ON_START_GAME = "game:status:start";
    public static final String ON_MAP_UPDATE = "game:state:update";
    public static final String ON_INVENTORY_UPDATE = "game:player:inventory:add";
    public static final String ON_INVENTORY_ADD= "game:player:inventory:add";
    public static final String ON_INVENTORY_CLEAR = "game:player:inventory:clear";
    public static final String ON_EFFECT_APPLY = "game:player:effects:apply";
    public static final String ON_EFFECT_CLEAR = "game:player:effects:clear";
    public static final String ON_PLAYER_REMOVE = "player:game:session:disconnected";

    //Emitting Event (SDK -> Server)
    public static final String EMIT_JOIN_GAME = "player:game:session:join";
    public static final String EMIT_LEAVE_GAME = "player:game:session:leave";
    public static final String EMIT_MOVE = "player:action:move";
    public static final String EMIT_ATTACK = "player:action:attack";
    public static final String EMIT_SHOOT = "player:action:shoot";
    public static final String EMIT_THROW = "player:action:throw";
    public static final String EMIT_PICKUP_ITEM = "player:item:pick";
    public static final String EMIT_USE_ITEM = "player:item:use";
    public static final String EMIT_USE_SPECIAL = "player:action:special";
    public static final String EMIT_REVOKE_ITEM = "player:item:drop";
}
