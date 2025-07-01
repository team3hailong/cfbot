package jsclub.codefest.sdk.socket.event_handler;

import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.model.GameMap;

public class onMapInit implements Emitter.Listener {
    private final GameMap gameMap;

    public onMapInit(GameMap gameMap) {
        this.gameMap = gameMap;
    }

    @Override
    public void call(Object... args) {
        gameMap.updateOnInitMap(args[0]);
    }
}
