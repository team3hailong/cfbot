package jsclub.codefest.sdk.model.npcs;

import jsclub.codefest.sdk.model.Element;
import jsclub.codefest.sdk.model.ElementType;

public class Enemy extends Element {
    private final int damage;

    public Enemy(String id, int damage) {
        super(id);
        this.damage = damage;
        this.setType(ElementType.ENEMY);
    }

    public int getDamage() {
        return damage;
    }
}
