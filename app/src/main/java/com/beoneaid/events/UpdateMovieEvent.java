package com.beoneaid.events;

import com.beoneaid.dao.MovieInfo;

import java.util.List;

/**
 * Created by wangfan on 2017/10/16.
 */

public class UpdateMovieEvent {

    private List<MovieInfo> movieList;
    private int movListIndex ;

    public UpdateMovieEvent(List<MovieInfo> ml,int mlidx){
        movieList = ml;
        movListIndex = mlidx;
    }

    public int getMovListIndex() {
        return movListIndex;
    }

    public List<MovieInfo> getMovieList() {
        return movieList;
    }
}
