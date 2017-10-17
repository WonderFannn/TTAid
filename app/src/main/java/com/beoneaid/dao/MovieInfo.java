package com.beoneaid.dao;

/**
 * Created by wangfan on 2017/8/28.
 */

public class MovieInfo {
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public String getPingy() {
        return pingy;
    }

    public void setPingy(String pingy) {
        this.pingy = pingy;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    private int id;
    private String title;
    private int cid;//分类
    private String pic;//图片url
    private String pingy;
    private String actor;
    private String director;//导演
    private int year;
    private float score;
}
