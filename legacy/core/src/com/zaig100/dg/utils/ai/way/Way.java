package com.zaig100.dg.utils.ai.way;

import com.zaig100.dg.world.World;

public class Way extends Coordinate {
    int iter;
    StartWay startWay;
    Coordinate backWay;
    String story;

    public Way(int x, int y,int iter, StartWay startWay) {
        super(x,y);
        this.iter = iter;
        this.startWay = startWay;
        this.backWay = startWay;
        init();
    }
    public Way(int x, int y,int iter, StartWay startWay,String story) {
        super(x,y);
        this.iter = iter;
        this.startWay = startWay;
        this.backWay = startWay;
        //this.story = story;
        addStory(story);
        init();
    }
    public Way(int x, int y,int iter,Coordinate backWay, StartWay startWay) {
        super(x,y);
        this.iter = iter;
        this.startWay = startWay;
        this.backWay =backWay;
        if(backWay instanceof Way) {
            addStory(((Way) backWay).getStory());
        }
        init();
    }

    @Override
    public void init() {

        if(iter>startWay.main.iterShort) {
            if ((x == startWay.getEndX()) && (y == startWay.getEndY())) { // проверка является ли координаты пути целью

                if (startWay.main.iterShort < iter) { // проверка на кротчайший путь
                    startWay.main.iterShort = iter;
                    startWay.setWayStory(story);
                }

                return;
            }

            for (int y = -1; y <= 1; y++) {

                for (int x = -1; x <= 1; x++) {

                    if (x == 0 && y == 0) continue;// пропуск повторной проверки самого себя

                    if (isBack(this.x+x, this.y+y)) continue; // пропуск проверки прошлой координаты

                    if (World.map.getTileId(this.x + x, this.y + y) == 11) {
                        //new Way(this.x + x, this.y + y, iter - 1,this, startWay)  // перебор окружащих путей
                        new Way(this.x + x, this.y + y, iter - 1,this, startWay); // перебор путей с сохранением истории

                    }

                }

            }

        }
    }

    public boolean isBack(int x,int y){
        return backWay.getX()==x&&backWay.getY()==y;
    }

    public void addStory(String story) {
        this.story = story + "\n" + iter + ":x=" + x + "y=" + y;
    }

    public void setStory(String story) {
        addStory(story);
    }

    public String getStory() {
        return story;
    }
}
