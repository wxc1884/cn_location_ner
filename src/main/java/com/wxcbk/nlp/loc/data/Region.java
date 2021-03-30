package com.wxcbk.nlp.loc.data;

import java.util.List;

/**
 * @author :owen
 * @Description :
 */
public class Region {
    /**
     * 区域的正式信息
     */
    private RegionPointAndEdge officialRegion;
    /**
     * 区域的级别
     */
    private int level;
    /**
     * 区域的正式名字，如北京市
     */
    private String normValue;
    /**
     * 区域的简短名字，如北京市的简短形式为北京
     */
    private String shortName;
    /**
     * 区域的后缀，如北京市的后缀为市
     */
    private String suffixName;
    /**
     * 区域的别名，如北京市的别名帝都
     */
    private List<String> nicknames;

    public RegionPointAndEdge getOfficialRegion() {
        return officialRegion;
    }

    public void setOfficialRegion(RegionPointAndEdge officialRegion) {
        this.officialRegion = officialRegion;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getNormValue() {
        return normValue;
    }

    public void setNormValue(String normValue) {
        this.normValue = normValue;
    }

    public String getSuffixName() {
        return suffixName;
    }

    public void setSuffixName(String suffixName) {
        this.suffixName = suffixName;
    }

    public List<String> getNicknames() {
        return nicknames;
    }

    public void setNicknames(List<String> nicknames) {
        this.nicknames = nicknames;
    }
}
