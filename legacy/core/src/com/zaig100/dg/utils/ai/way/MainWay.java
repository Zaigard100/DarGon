package com.zaig100.dg.utils.ai.way;

import com.zaig100.dg.world.World;

import java.util.ArrayList;

public class MainWay extends Coordinate{

    int iter;
    int endX,endY;
    ArrayList<StartWay> startWays = new ArrayList<>();
    public int iterShort;

    public MainWay(int x, int y,int endX, int endY, int iter) {
        super(x, y);
        this.iter = iter;
        this.endX = endX;
        this.endY = endY;
    }

    @Override
    public void init() {
        for (int y = -1; y <= 1; y++) {

            for (int x = -1; x <= 1; x++) {

                if (x == 0 && y == 0) continue; // пропуск проверки самого себя

                if (World.map.getTileId(this.x + x, this.y + y) == 11) {
                    startWays.add(new StartWay(this.x + x, this.y + y, endX, endY, iter,this)); // добавление окружащих путей
                }

            }

        }
    }

    public Coordinate shortWay(){
        int lenght = -2;
        Coordinate ret = new NullWay();
        for(StartWay way:startWays){
            way.init();
            if(way.x == endX&&way.y == endY) return way;
            if(way.main.iterShort>lenght){
                lenght =way.main.iterShort;
                ret = way;
            }
        }
        if(lenght>0) return ret;
        else return new NullWay();
    }

}
