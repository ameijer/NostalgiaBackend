package com.nostalgia.persistence.model;


import java.io.Serializable;

import org.geojson.Point;

/**
 * Created by alex on 11/8/15.
 */
public class LocationUpdate implements Serializable {
    private Point location;
    private String userId;

    public LocationUpdate( Point location, String userId) {
        this.location = location;
        this.userId = userId;
    }

    public LocationUpdate(){}


    public  Point getLocation() {
        return location;
    }

    public void setLocation( Point location) {
        this.location = location;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }


}
