package com.wxcbk.nlp.loc.data;

/**
 * @author :owen
 * @Description :
 */
public class LocCandidate {

    private RegionPointAndEdge country;
    private RegionPointAndEdge province;
    private RegionPointAndEdge city;
    private RegionPointAndEdge county;
    private RegionPointAndEdge town;
    private int level;


    public RegionPointAndEdge getRegionPoint() {
        switch (level) {
            case 1:
                return country;
            case 2:
                return province;
            case 3:
                return city;
            case 4:
                return county;
            case 5:
                return town;
            default:
        }
        return null;
    }

    public void setRegionPoint(RegionPointAndEdge regionPoint) {
        int level = regionPoint.getLevel();
        switch (level) {
            case 5:
                this.town = regionPoint;
                break;
            case 4:
                this.county = regionPoint;
                break;
            case 3:
                this.city = regionPoint;
                break;
            case 2:
                this.province = regionPoint;
            case 1:
                this.country = regionPoint;
                break;
            default:
        }
    }

    public boolean containMultiRegion() {
        int num = 0;
        if (town != null) {
            num++;
        }
        if (county != null) {
            num++;
        }
        if (city != null) {
            num++;
        }
        if (province != null) {
            num++;
        }
        return num > 1;
    }

    public RegionPointAndEdge getCountry() {
        return country;
    }

    public void setCountry(RegionPointAndEdge country) {
        this.country = country;
    }

    public RegionPointAndEdge getProvince() {
        return province;
    }

    public void setProvince(RegionPointAndEdge province) {
        this.province = province;
    }

    public RegionPointAndEdge getCity() {
        return city;
    }

    public void setCity(RegionPointAndEdge city) {
        this.city = city;
    }

    public RegionPointAndEdge getCounty() {
        return county;
    }

    public void setCounty(RegionPointAndEdge county) {
        this.county = county;
    }

    public RegionPointAndEdge getTown() {
        return town;
    }

    public void setTown(RegionPointAndEdge town) {
        this.town = town;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
