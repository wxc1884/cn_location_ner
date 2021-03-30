package com.wxcbk.nlp.loc.data;

import java.util.List;

/**
 * @author :owen
 * @Description :
 */
public class LocationToken {

    private String text;
    private int begin;
    private int end;
    private List<Location> locations;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }
}
