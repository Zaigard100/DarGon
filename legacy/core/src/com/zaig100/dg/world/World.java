package com.zaig100.dg.world;

public class World {

    public static Player player;
    public static Map map;

    static{
        player = new Player(0,0);
        map = null;
    }
    public static void del(){
        map.del();
    }
    public static void setMap(Map map) {
        World.map = map;
    }
    public Map getMap() {
        return map;
    }

    public static void setPlayer(Player player) {
        World.player = player;
    }
}
