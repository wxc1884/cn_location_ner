package com.wxcbk.nlp.loc.data;

import java.io.Serializable;

/**
 * @author :owen
 * @Description :
 */
public class RegionPointAndEdge implements Serializable {

    /**
     * 区域级别
     */
    private int level;
    /**
     * 区域编号
     */
    private String locCode;
    /**
     * 隶属的2级区域即省级区域
     */
    private String preLevel2;
    /**
     * 隶属的3级区域即市级区域
     */
    private String preLevel3;
    /**
     * 隶属的4级区域即县级区域
     */
    private String preLevel4;
    /**
     * text如 北京
     */
    private String value;
    private String normValue;
    /**
     * 是否删除，如北京，2级区域
     * 是北京市，三级区域也是北京市
     * 最后删除2级区域
     */
    private boolean remove;
    /**
     * 查询text在query的起始位置
     */
    private int start;

    public RegionPointAndEdge() {
    }

    public String getLocCodeWithPos() {
        return locCode + ":" + start;
    }

    public String getTextWithPos() {
        return value + ":" + start;
    }

    public RegionPointAndEdge(int level, String locCode, String preLevel2, String preLevel3, String preLevel4) {
        this.level = level;
        this.locCode = locCode;
        this.preLevel2 = preLevel2;
        this.preLevel3 = preLevel3;
        this.preLevel4 = preLevel4;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getLocCode() {
        return locCode;
    }

    public void setLocCode(String locCode) {
        this.locCode = locCode;
    }

    public void setPreLocCode(String preLocCode) {
        switch (level) {
            case 5:
                setPreLevel4("4_" + preLocCode);
                break;
            case 4:
                setPreLevel3("3_" + preLocCode);
                break;
            case 3:
                setPreLevel2("2_" + preLocCode);
                break;
            case 2:
            default:
        }

    }

    public String getPreLevel2() {
        return preLevel2;
    }

    public void setPreLevel2(String preLevel2) {
        this.preLevel2 = preLevel2;
    }

    public String getPreLevel3() {
        return preLevel3;
    }

    public void setPreLevel3(String preLevel3) {
        this.preLevel3 = preLevel3;
    }

    public String getPreLevel4() {
        return preLevel4;
    }

    public void setPreLevel4(String preLevel4) {
        this.preLevel4 = preLevel4;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public String getNormValue() {
        return normValue;
    }

    public void setNormValue(String normValue) {
        this.normValue = normValue;
    }

    public boolean isRemove() {
        return remove;
    }

    public void setRemove(boolean remove) {
        this.remove = remove;
    }

    @Override
    public String toString() {
        return "RegionPoint{" +
                "level=" + level +
                ", locCode=" + locCode +
                ", preLevel2=" + preLevel2 +
                ", preLevel3=" + preLevel3 +
                ", preLevel4=" + preLevel4 +
                ", start=" + start +
                ", value='" + value + '\'' +
                '}';
    }
}
