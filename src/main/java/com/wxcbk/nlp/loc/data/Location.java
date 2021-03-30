package com.wxcbk.nlp.loc.data;

/**
 * @author :owen
 * @Description :
 */
public class Location {
    /**
     * 国家
     */
    private String country;
    /**
     * 省级行政区域
     */
    private String province;
    /**
     * 地市级行政区域
     */
    private String city;
    /**
     * 区县级行政区域
     */
    private String county;
    /**
     * 乡镇级行政区域
     */
    private String town;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }
}
