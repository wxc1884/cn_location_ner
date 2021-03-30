package com.wxcbk.nlp.loc.ner;

import com.alibaba.fastjson.JSON;
import com.wxcbk.nlp.loc.data.constants.Constant;
import com.wxcbk.nlp.loc.ptct.NodeItem;
import com.wxcbk.nlp.loc.data.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author :owen
 * @Description :
 */
public class CnLocRecognizer {

    private static final Logger log = LoggerFactory.getLogger(CnLocRecognizer.class);

    private LocRegionTrie locRegionTrie;
    private Map<String, Region> regionMap;
    private Pattern pattern = Pattern.compile("NAN");

    public CnLocRecognizer() {
        this.locRegionTrie = new LocRegionTrie();
        this.locRegionTrie.buildTrie();
        this.regionMap = this.locRegionTrie.getRegionMap();

    }


    public void addLocNickname(String filePath) {
        this.locRegionTrie.addLocNicknameToTrie(filePath);
    }

    public void addLocNickName(Map<String, List<String>> normAndSynWords) {

    }

    public List<LocationToken> locNer(String logTraceId, String query) {
        if (StringUtils.isEmpty(logTraceId)) {
            logTraceId = "[loc_ner]";
        }
        List<NodeItem<RegionPointAndEdge>> regionInfoList = extractRegionInfo(logTraceId, query);
        List<LocationToken> locations = regionInfoToLoc(logTraceId, regionInfoList, query);
        log.info("{}:loc:{}", logTraceId, JSON.toJSONString(locations));
        return locations;
    }

    private List<LocationToken> regionInfoToLoc(String logTraceId, List<NodeItem<RegionPointAndEdge>> regionInfoList, String query) {
        List<LocationToken> locList = new ArrayList<>();
        if (CollectionUtils.isEmpty(regionInfoList)) {
            return locList;
        }
        log.debug("{}:loc regions:{}", logTraceId, logDisplayRegion(regionInfoList));
        Map<Integer, List<NodeItem<RegionPointAndEdge>>> levelToRegionInfo = transToLevelMapRegionInfo(regionInfoList);
        Map<String, NodeItem<RegionPointAndEdge>> locCodeToRegionInfo = transToCodeMapRegionInfo(regionInfoList);
        setRegionInfoRemoveFlag(regionInfoList, levelToRegionInfo, locCodeToRegionInfo);
        this.combineRegionToLoc(query, levelToRegionInfo, locCodeToRegionInfo, locList);
        return locList;
    }


    private void setRegionInfoRemoveFlag(List<NodeItem<RegionPointAndEdge>> regionInfoList, Map<Integer, List<NodeItem<RegionPointAndEdge>>> levelToRegionInfo, Map<String, NodeItem<RegionPointAndEdge>> locCodeToRegionInfo) {
        if (CollectionUtils.isNotEmpty(levelToRegionInfo.get(Constant.LEVEL_4))) {
            levelToRegionInfo.get(Constant.LEVEL_4).forEach(nodeItem -> {
                if (locCodeToRegionInfo.containsKey(nodeItem.getNodeValue().getPreLevel3())) {
                    if (regionMap.get(nodeItem.getNodeValue().getPreLevel3()).getNormValue().equals(regionMap.get(nodeItem.getNodeValue().getLocCode()).getNormValue())) {
                        nodeItem.getNodeValue().setRemove(true);
                    }
                }
            });
        }
        if (CollectionUtils.isNotEmpty(levelToRegionInfo.get(Constant.LEVEL_3))) {
            levelToRegionInfo.get(Constant.LEVEL_3).forEach(nodeItem -> {
                if (locCodeToRegionInfo.containsKey(nodeItem.getNodeValue().getPreLevel2())) {
                    if (regionMap.get(nodeItem.getNodeValue().getPreLevel2()).getNormValue().equals(regionMap.get(nodeItem.getNodeValue().getLocCode()).getNormValue())) {
                        locCodeToRegionInfo.get(nodeItem.getNodeValue().getPreLevel2()).getNodeValue().setRemove(true);
                    }
                }
            });
        }
    }

    private Map<Integer, List<NodeItem<RegionPointAndEdge>>> transToLevelMapRegionInfo(List<NodeItem<RegionPointAndEdge>> regionInfoList) {
        Map<Integer, List<NodeItem<RegionPointAndEdge>>> levelRegionMap = new HashMap<>(16);
        regionInfoList.forEach(nodeItem -> {
            RegionPointAndEdge regionPoint = nodeItem.getNodeValue();
            regionPoint.setStart(nodeItem.getBegin());
            Integer level = regionPoint.getLevel();
            if (levelRegionMap.containsKey(level)) {
                levelRegionMap.get(level).add(nodeItem);
            } else {
                List<NodeItem<RegionPointAndEdge>> nodeItems = new ArrayList<>();
                nodeItems.add(nodeItem);
                levelRegionMap.put(level, nodeItems);
            }
        });
        return levelRegionMap;
    }

    private Map<String, NodeItem<RegionPointAndEdge>> transToCodeMapRegionInfo(List<NodeItem<RegionPointAndEdge>> regionInfoList) {
        Map<String, NodeItem<RegionPointAndEdge>> codeRegionMap = new HashMap<>(16);
        regionInfoList.forEach(nodeItem -> codeRegionMap.put(nodeItem.getNodeValue().getLocCode(), nodeItem));
        return codeRegionMap;
    }

    private void combineRegionToLoc(String query, Map<Integer, List<NodeItem<RegionPointAndEdge>>> levelToRegionInfo, Map<String, NodeItem<RegionPointAndEdge>> locCodeToRegionInfo, List<LocationToken> locList) {
        List<LocCandidate> locCandidates = new ArrayList<>();
        if (levelToRegionInfo.containsKey(Constant.LEVEL_5)) {
            extractCandidateRegion(levelToRegionInfo, Constant.LEVEL_5, locCodeToRegionInfo, locCandidates);
        }
        if (levelToRegionInfo.containsKey(Constant.LEVEL_4)) {
            extractCandidateRegion(levelToRegionInfo, Constant.LEVEL_4, locCodeToRegionInfo, locCandidates);
        }
        if (levelToRegionInfo.containsKey(Constant.LEVEL_3)) {
            extractCandidateRegion(levelToRegionInfo, Constant.LEVEL_3, locCodeToRegionInfo, locCandidates);
        }
        if (levelToRegionInfo.containsKey(Constant.LEVEL_2)) {
            extractCandidateRegion(levelToRegionInfo, Constant.LEVEL_2, locCodeToRegionInfo, locCandidates);
        }
        transCandidateToLoc(query, locCandidates, locList);
    }

    /**
     * 候选行政区域消歧，联立组合
     *
     * @param query
     * @param locCandidates
     * @param locList
     */
    private void transCandidateToLoc(String query, List<LocCandidate> locCandidates, List<LocationToken> locList) {
        if (CollectionUtils.isEmpty(locCandidates)) {
            return;
        }
        List<LocCandidate> combinedCandidates = new ArrayList<>();
        Map<String, LocCandidate> locCodeToCandidates = transLocCodePosCandidateMap(locCandidates);
        Map<String, List<LocCandidate>> textToCandidates = transTextPosCandidateMap(locCandidates);
        Map<Integer, List<LocCandidate>> levelToCandidates = transLevelCandidateMap(locCandidates);
        //筛选可以组合在一起的地点
        selectCombinedCandidate(query, levelToCandidates, combinedCandidates, locCodeToCandidates);
        //筛选组合后独立的候选地点
        List<LocCandidate> independentLocCandidates = selectIndependentLocCandidate(textToCandidates, combinedCandidates);
        Map<String, LocationToken> textToLocationToken = new HashMap<>();
        independentLocCandidates.forEach(candidate -> assemblyLocationToken(candidate, locList, textToLocationToken));
        //地点token过滤，选出最佳的地点集合
        postFilterBestLocTokens(locList);
    }

    private void postFilterBestLocTokens(List<LocationToken> locList) {
        locList.sort((loc1, loc2) -> {
            if (loc1.getText().length() - loc2.getText().length() > 0) {
                return -1;
            } else if (loc1.getText().length() - loc2.getText().length() < 0) {
                return 1;
            }
            return 0;
        });
        List<LocationToken> bestTokens = new ArrayList<>();
        List<LocationToken> removeTokens = new ArrayList<>();
        for (LocationToken locToken : locList) {
            if (CollectionUtils.isEmpty(bestTokens)) {
                bestTokens.add(locToken);
                continue;
            }
            int locStart = locToken.getBegin();
            int locEnd = locToken.getEnd();
            for (LocationToken bestToken : bestTokens) {
                int bestStart = bestToken.getBegin();
                int bestEnd = bestToken.getEnd();
                if (locStart >= bestStart && locEnd <= bestEnd) {
                    removeTokens.add(locToken);
                    break;
                }
            }
        }
        if (removeTokens.size() > 0) {
            removeTokens.forEach(locList::remove);
        }
    }

    private List<LocCandidate> selectIndependentLocCandidate(Map<String, List<LocCandidate>> textToCandidates, List<LocCandidate> multiCodeCandidates) {
        List<LocCandidate> independentLocCandidates = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(multiCodeCandidates)) {
            independentLocCandidates.addAll(multiCodeCandidates);
            List<String> textKeys = fetchTexKeys(multiCodeCandidates);
            textKeys.forEach(textToCandidates::remove);
        }
        textToCandidates.forEach((text, candidates) -> {
            boolean hasSuffix = false;
            for (LocCandidate candidate : candidates) {
                if (!candidate.getRegionPoint().getValue().equals(candidate.getRegionPoint().getNormValue())) {
                    hasSuffix = true;
                    if (!candidate.getRegionPoint().isRemove()) {
                        independentLocCandidates.add(candidate);
                    }

                }
            }
            if (!hasSuffix) {
                candidates.forEach(candidate -> {
                    if (!candidate.getRegionPoint().isRemove()) {
                        independentLocCandidates.add(candidate);
                    }
                });
            }
        });
        return independentLocCandidates;
    }

    private List<String> fetchTexKeys(List<LocCandidate> multiCodeCandidates) {
        List<String> textKeys = new ArrayList<>();
        multiCodeCandidates.forEach(locCandidate -> extractTextKeys(locCandidate, textKeys));
        return textKeys;
    }


    private void extractTextKeys(LocCandidate candidate, List<String> textKeys) {
        if (candidate.getTown() != null) {
            textKeys.add(candidate.getTown() + ":" + candidate.getTown().getStart());
        }
        if (candidate.getCounty() != null) {
            textKeys.add(candidate.getCounty().getValue() + ":" + candidate.getCounty().getStart());
        }
        if (candidate.getCity() != null) {
            textKeys.add(candidate.getCity().getValue() + ":" + candidate.getCity().getStart());
        }
        if (candidate.getProvince() != null) {
            textKeys.add(candidate.getProvince().getValue() + ":" + candidate.getProvince().getStart());
        }
    }


    private void selectCombinedCandidate(String query, Map<Integer, List<LocCandidate>> levelToCandidates, List<LocCandidate> multiCodeCandidates, Map<String, LocCandidate> locCodeToCandidates) {
        List<String> handledLocCodes = new ArrayList<>();
        if (levelToCandidates.containsKey(Constant.LEVEL_5)) {
            filterMultiCodeCandidates(query, levelToCandidates.get(Constant.LEVEL_5), multiCodeCandidates, locCodeToCandidates, handledLocCodes);
        }
        if (levelToCandidates.containsKey(Constant.LEVEL_4)) {
            filterMultiCodeCandidates(query, levelToCandidates.get(Constant.LEVEL_4), multiCodeCandidates, locCodeToCandidates, handledLocCodes);
        }
        if (levelToCandidates.containsKey(Constant.LEVEL_3)) {
            filterMultiCodeCandidates(query, levelToCandidates.get(Constant.LEVEL_3), multiCodeCandidates, locCodeToCandidates, handledLocCodes);
        }
        if (levelToCandidates.containsKey(Constant.LEVEL_2)) {
            filterMultiCodeCandidates(query, levelToCandidates.get(Constant.LEVEL_2), multiCodeCandidates, locCodeToCandidates, handledLocCodes);
        }
    }


    private void filterMultiCodeCandidates(String query, List<LocCandidate> candidates, List<LocCandidate> multiCodeCandidates, Map<String, LocCandidate> locCodeToCandidates, List<String> handledLocCodes) {
        if (CollectionUtils.isEmpty(candidates)) {
            return;
        }
        for (LocCandidate locCandidate : candidates) {
            if (handledLocCodes.contains(locCandidate.getRegionPoint().getLocCodeWithPos())) {
                continue;
            }
            reFillCandidateByQuery(query, locCandidate, locCodeToCandidates, handledLocCodes);
        }
        for (LocCandidate locCandidate : candidates) {
            if (locCandidate.containMultiRegion()) {
                multiCodeCandidates.add(locCandidate);
            }
        }
    }

    private void reFillCandidateByQuery(String query, LocCandidate locCandidate, Map<String, LocCandidate> locCodeToCandidates, List<String> handledLocCodes) {
        Pattern pattern = generatePatternByCandidate(locCandidate);
        Matcher m = pattern.matcher(query);
        fillCandidate(m, locCandidate, locCodeToCandidates, handledLocCodes);
    }

    private void fillCandidate(Matcher matcher, LocCandidate locCandidate, Map<String, LocCandidate> locCodeToCandidates, List<String> handledLocCodes) {
        int level = locCandidate.getRegionPoint().getLevel();
        while (matcher.find()) {
            switch (level) {
                case 5:
                    locCandidate = locCodeToCandidates.get(locCandidate.getRegionPoint().getLocCode() + ":" + matcher.start("town"));
                    handledLocCodes.add(locCandidate.getRegionPoint().getLocCodeWithPos());
                    fillLevel5Candidate(matcher, locCandidate, locCodeToCandidates, handledLocCodes);
                    break;
                case 4:
                    locCandidate = locCodeToCandidates.get(locCandidate.getRegionPoint().getLocCode() + ":" + matcher.start("county"));
                    handledLocCodes.add(locCandidate.getRegionPoint().getLocCodeWithPos());
                    fillLevel4Candidate(matcher, locCandidate, locCodeToCandidates, handledLocCodes);
                    break;
                case 3:
                    locCandidate = locCodeToCandidates.get(locCandidate.getRegionPoint().getLocCode() + ":" + matcher.start("city"));
                    handledLocCodes.add(locCandidate.getRegionPoint().getLocCodeWithPos());
                    fillLevel3Candidate(matcher, locCandidate, locCodeToCandidates, handledLocCodes);
                    break;
                case 2:
                    locCandidate = locCodeToCandidates.get(locCandidate.getRegionPoint().getLocCode() + ":" + matcher.start("province"));
                    handledLocCodes.add(locCandidate.getRegionPoint().getLocCodeWithPos());
                    fillLevel2Candidate(matcher, locCandidate);
                default:
            }
        }
    }


    private void fillLevel5Candidate(Matcher matcher, LocCandidate locCandidate, Map<String, LocCandidate> locCodeToCandidates, List<String> handledLocCodes) {
        if (StringUtils.isNotEmpty(matcher.group("town"))) {
            locCandidate.getRegionPoint().setNormValue(matcher.group("town"));
        }
        if (StringUtils.isNotEmpty(matcher.group("county"))) {
            RegionPointAndEdge county = locCodeToCandidates.get(locCandidate.getRegionPoint().getPreLevel4() + ":" + matcher.start("county")).getRegionPoint();
            county.setNormValue(matcher.group("county"));
            locCandidate.setCounty(county);
            handledLocCodes.add(county.getLocCodeWithPos());
        }
        if (StringUtils.isNotEmpty(matcher.group("city"))) {
            RegionPointAndEdge city = locCodeToCandidates.get(locCandidate.getRegionPoint().getPreLevel3() + ":" + matcher.start("city")).getRegionPoint();
            city.setNormValue(matcher.group("city"));
            locCandidate.setCity(city);
            handledLocCodes.add(city.getLocCodeWithPos());
        }
        if (StringUtils.isNotEmpty(matcher.group("province"))) {
            RegionPointAndEdge province = locCodeToCandidates.get(locCandidate.getRegionPoint().getPreLevel2() + ":" + matcher.start("province")).getRegionPoint();
            province.setNormValue(matcher.group("province"));
            locCandidate.setProvince(province);
            handledLocCodes.add(province.getLocCodeWithPos());
        }
    }

    private void fillLevel4Candidate(Matcher matcher, LocCandidate locCandidate, Map<String, LocCandidate> locCodeToCandidates, List<String> handledLocCodes) {
        if (StringUtils.isNotEmpty(matcher.group("county"))) {
            locCandidate.getRegionPoint().setNormValue(matcher.group("county"));
        }
        if (StringUtils.isNotEmpty(matcher.group("city"))) {
            RegionPointAndEdge city = locCodeToCandidates.get(locCandidate.getRegionPoint().getPreLevel3() + ":" + matcher.start("city")).getRegionPoint();
            city.setNormValue(matcher.group("city"));
            locCandidate.setCity(city);
            handledLocCodes.add(city.getLocCodeWithPos());
        }
        if (StringUtils.isNotEmpty(matcher.group("province"))) {
            RegionPointAndEdge province = locCodeToCandidates.get(locCandidate.getRegionPoint().getPreLevel2() + ":" + matcher.start("province")).getRegionPoint();
            province.setNormValue(matcher.group("province"));
            locCandidate.setProvince(province);
            handledLocCodes.add(province.getLocCodeWithPos());
        }
    }

    private void fillLevel3Candidate(Matcher matcher, LocCandidate locCandidate, Map<String, LocCandidate> locCodeToCandidates, List<String> handledLocCodes) {
        if (StringUtils.isNotEmpty(matcher.group("city"))) {
            locCandidate.getRegionPoint().setNormValue(matcher.group("city"));
        }
        if (StringUtils.isNotEmpty(matcher.group("province"))) {
            RegionPointAndEdge province = locCodeToCandidates.get(locCandidate.getRegionPoint().getPreLevel2() + ":" + matcher.start("province")).getRegionPoint();
            province.setNormValue(matcher.group("province"));
            locCandidate.setProvince(province);
            handledLocCodes.add(province.getLocCodeWithPos());
        }
    }

    private void fillLevel2Candidate(Matcher matcher, LocCandidate locCandidate) {
        if (StringUtils.isNotEmpty(matcher.group("province"))) {
            locCandidate.getRegionPoint().setNormValue(matcher.group("province"));
        }
    }


    private Pattern generatePatternByCandidate(LocCandidate locCandidate) {
        RegionPointAndEdge regionPoint = locCandidate.getRegionPoint();
        int level = regionPoint.getLevel();
        switch (level) {
            case 5:
                return genLevel5Pattern(regionPoint);
            case 4:
                return genLevel4Pattern(regionPoint);
            case 3:
                return genLevel3Pattern(regionPoint);
            case 2:
                return genLevel2Pattern(regionPoint);
            default:
        }
        return pattern;
    }

    private Pattern genLevel5Pattern(RegionPointAndEdge regionPoint) {
        String province = generateLocCodePatternValue(regionPoint.getPreLevel2()) + "(" + regionMap.get(regionPoint.getPreLevel2()).getSuffixName() + ")?";
        String city = generateLocCodePatternValue(regionPoint.getPreLevel3()) + "(" + regionMap.get(regionPoint.getPreLevel3()).getSuffixName() + ")?";
        String county = generateLocCodePatternValue(regionPoint.getPreLevel4()) + "(" + regionMap.get(regionPoint.getPreLevel4()).getSuffixName() + ")?";
        String town = regionPoint.getValue() + "(" + regionMap.get(regionPoint.getLocCode()).getSuffixName() + ")?";
        return Pattern.compile("(?<province>" + province + ")?" + "(?<city>" + city + ")?" + "(?<county>" + county + ")" + "(?<town>" + town + ")");
    }

    private Pattern genLevel4Pattern(RegionPointAndEdge regionPoint) {
        String province = generateLocCodePatternValue(regionPoint.getPreLevel2()) + "(" + regionMap.get(regionPoint.getPreLevel2()).getSuffixName() + ")?";
        String city = generateLocCodePatternValue(regionPoint.getPreLevel3()) + "(" + regionMap.get(regionPoint.getPreLevel3()).getSuffixName() + ")?";
        String county = regionPoint.getValue() + "(" + regionMap.get(regionPoint.getLocCode()).getSuffixName() + ")?";
        return Pattern.compile("(?<province>" + province + ")?" + "(?<city>" + city + ")?" + "(?<county>" + county + ")");
    }

    private Pattern genLevel3Pattern(RegionPointAndEdge regionPoint) {
        String province = generateLocCodePatternValue(regionPoint.getPreLevel2()) + "(" + regionMap.get(regionPoint.getPreLevel2()).getSuffixName() + ")?";
        String city = regionPoint.getValue() + "(" + regionMap.get(regionPoint.getLocCode()).getSuffixName() + ")?";
        return Pattern.compile("(?<province>" + province + ")?" + "(?<city>" + city + ")");
    }

    private Pattern genLevel2Pattern(RegionPointAndEdge regionPoint) {
        String province = regionPoint.getValue() + "(" + regionMap.get(regionPoint.getLocCode()).getSuffixName() + ")?";
        return Pattern.compile("(?<province>" + province + ")");
    }

    private String generateLocCodePatternValue(String locCode) {
        List<String> synWords = regionMap.get(locCode).getNicknames();
        if (CollectionUtils.isEmpty(synWords)) {
            return regionMap.get(locCode).getShortName();
        }
        List<String> synAndNorm = new ArrayList<>(synWords);
        synAndNorm.add(regionMap.get(locCode).getShortName());
        synAndNorm.sort((o1, o2) -> {
            if (o1.length() - o2.length() > 0) {
                return -1;
            } else if (o1.length() - o2.length() < 0) {
                return 1;
            }
            return 0;
        });
        return "(" + String.join("|", synAndNorm) + ")";
    }


    private Map<String, List<LocCandidate>> transTextPosCandidateMap(List<LocCandidate> locCandidates) {
        Map<String, List<LocCandidate>> map = new HashMap<>();
        locCandidates.forEach(locCandidate -> {
            if (map.containsKey(locCandidate.getRegionPoint().getTextWithPos())) {
                map.get(locCandidate.getRegionPoint().getTextWithPos()).add(locCandidate);
            } else {
                List<LocCandidate> list = new ArrayList<>();
                list.add(locCandidate);
                map.put(locCandidate.getRegionPoint().getTextWithPos(), list);
            }
        });
        return map;
    }

    /**
     * @param locCandidates key为locCode+text在query中的起始位置
     * @return
     */
    private Map<String, LocCandidate> transLocCodePosCandidateMap(List<LocCandidate> locCandidates) {
        Map<String, LocCandidate> map = new HashMap<>();
        locCandidates.forEach(locCandidate -> map.put(locCandidate.getRegionPoint().getLocCodeWithPos(), locCandidate));
        return map;
    }

    private Map<Integer, List<LocCandidate>> transLevelCandidateMap(List<LocCandidate> locCandidates) {
        Map<Integer, List<LocCandidate>> map = new HashMap<>();
        locCandidates.forEach(locCandidate -> {
            if (map.containsKey(locCandidate.getRegionPoint().getLevel())) {
                map.get(locCandidate.getRegionPoint().getLevel()).add(locCandidate);
            } else {
                List<LocCandidate> list = new ArrayList<>();
                list.add(locCandidate);
                map.put(locCandidate.getRegionPoint().getLevel(), list);
            }
        });
        return map;
    }

    private void assemblyLocationToken(LocCandidate locCandidate, List<LocationToken> locList, Map<String, LocationToken> textToLocationToken) {
        String textAndPos = fetchTextPosByCandidate(locCandidate);
        if (textToLocationToken.containsKey(textAndPos)) {
            Location location = assemblyLocation(locCandidate);
            textToLocationToken.get(textAndPos).getLocations().add(location);
            return;
        }
        LocationToken locationToken = new LocationToken();
        List<Location> locations = new ArrayList<>();
        locations.add(assemblyLocation(locCandidate));
        String[] texts = textAndPos.split("_");
        locationToken.setText(texts[0]);
        locationToken.setBegin(Integer.parseInt(texts[1]));
        locationToken.setEnd(Integer.parseInt(texts[1]) + texts[0].length());
        locationToken.setLocations(locations);
        locList.add(locationToken);
        textToLocationToken.put(textAndPos, locationToken);
    }

    private Location assemblyLocation(LocCandidate candidate) {
        Location location = new Location();
        int level = candidate.getLevel();
        switch (level) {
            case 5:
                location.setTown(regionMap.get(candidate.getTown().getLocCode()).getNormValue());
                location.setCounty(regionMap.get(candidate.getTown().getPreLevel4()).getNormValue());
                location.setCity(regionMap.get(candidate.getTown().getPreLevel3()).getNormValue());
                location.setProvince(regionMap.get(candidate.getTown().getPreLevel2()).getNormValue());
                location.setCountry(Constant.SIMPLE_CHINA_NAME);
                break;
            case 4:
                location.setCounty(regionMap.get(candidate.getCounty().getLocCode()).getNormValue());
                location.setCity(regionMap.get(candidate.getCounty().getPreLevel3()).getNormValue());
                location.setProvince(regionMap.get(candidate.getCounty().getPreLevel2()).getNormValue());
                location.setCountry(Constant.SIMPLE_CHINA_NAME);
                break;
            case 3:
                location.setCity(regionMap.get(candidate.getCity().getLocCode()).getNormValue());
                location.setProvince(regionMap.get(candidate.getCity().getPreLevel2()).getNormValue());
                location.setCountry(Constant.SIMPLE_CHINA_NAME);
                break;
            case 2:
                location.setProvince(regionMap.get(candidate.getProvince().getLocCode()).getNormValue());
                location.setCountry(Constant.SIMPLE_CHINA_NAME);
                break;
            default:
        }
        return location;
    }

    private String fetchTextPosByCandidate(LocCandidate locCandidate) {
        StringBuilder text = new StringBuilder();
        int start = -1;
        boolean hasPos = false;
        if (locCandidate.getProvince() != null) {
            start = locCandidate.getProvince().getStart();
            hasPos = true;
            text.append(locCandidate.getProvince().getNormValue());
        }
        if (locCandidate.getCity() != null) {
            if (!hasPos) {
                start = locCandidate.getCity().getStart();
            }
            text.append(locCandidate.getCity().getNormValue());
        }
        if (locCandidate.getCounty() != null) {
            if (!hasPos) {
                start = locCandidate.getCounty().getStart();
            }
            text.append(locCandidate.getCounty().getNormValue());
        }
        if (locCandidate.getTown() != null) {
            if (!hasPos) {
                start = locCandidate.getTown().getStart();
            }
            text.append(locCandidate.getTown().getNormValue());
        }
        if (start != -1) {
            return text.toString() + "_" + start;
        }
        return text.toString();
    }

    /**
     * 选择候选的行政区域
     *
     * @param levelToRegionInfo
     * @param level
     * @param locCodeToRegionInfo
     * @param locCandidates
     */
    private void extractCandidateRegion(Map<Integer, List<NodeItem<RegionPointAndEdge>>> levelToRegionInfo,
                                        int level, Map<String, NodeItem<RegionPointAndEdge>> locCodeToRegionInfo, List<LocCandidate> locCandidates) {
        List<NodeItem<RegionPointAndEdge>> levelRegions = levelToRegionInfo.get(level);
        if (CollectionUtils.isEmpty(levelRegions)) {
            return;
        }
        levelRegions.forEach(region -> addCandidateRegion(region, level, locCodeToRegionInfo, locCandidates));
    }

    private List<NodeItem<RegionPointAndEdge>> filterNoHandledRegion
            (List<NodeItem<RegionPointAndEdge>> levelRegions, List<String> handledLocCodes) {
        List<NodeItem<RegionPointAndEdge>> nodeItems = new ArrayList<>();
        levelRegions.forEach(region -> {
            if (!handledLocCodes.contains(region.getNodeValue().getLocCode())) {
                nodeItems.add(region);
            }
        });
        return nodeItems;
    }

    private void addCandidateRegion(NodeItem<RegionPointAndEdge> region, int level, Map<
            String, NodeItem<RegionPointAndEdge>> locCodeToRegionInfo, List<LocCandidate> locCandidates) {
        RegionPointAndEdge regionPoint = region.getNodeValue();
        switch (level) {
            case Constant.LEVEL_5:
                if (locCodeToRegionInfo.containsKey(regionPoint.getPreLevel4())) {
                    LocCandidate locCandidateL5 = new LocCandidate();
                    locCandidateL5.setTown(regionPoint);
                    locCandidateL5.setLevel(Constant.LEVEL_5);
                    locCandidates.add(locCandidateL5);
                }
                break;
            case Constant.LEVEL_4:
                LocCandidate locCandidateL4 = new LocCandidate();
                locCandidateL4.setCounty(regionPoint);
                locCandidateL4.setLevel(Constant.LEVEL_4);
                locCandidates.add(locCandidateL4);
                break;
            case Constant.LEVEL_3:
                LocCandidate locCandidateL3 = new LocCandidate();
                locCandidateL3.setCity(regionPoint);
                locCandidateL3.setLevel(Constant.LEVEL_3);
                locCandidates.add(locCandidateL3);
                break;
            case Constant.LEVEL_2:
                LocCandidate locCandidateL2 = new LocCandidate();
                locCandidateL2.setProvince(regionPoint);
                locCandidateL2.setLevel(Constant.LEVEL_2);
                locCandidates.add(locCandidateL2);
                break;
            default:
        }
    }

    private List<NodeItem<RegionPointAndEdge>> extractRegionInfo(String logTraceId, String query) {
        long startTime = System.currentTimeMillis();
        List<NodeItem<RegionPointAndEdge>> regionInfoList = this.locRegionTrie.getPatriciaTrie().search(query);
        log.info("{}：loc search time:{}", logTraceId, System.currentTimeMillis() - startTime);
        return regionInfoList;
    }


    private List<String> logDisplayRegion(List<NodeItem<RegionPointAndEdge>> regionInfoList) {
        List<String> regions = new ArrayList<>();
        regionInfoList.forEach(item -> {
            String value = "(" + item.getTextValue() + "|" + item.getNodeValue().getValue() + "<" + item.getBegin() + "," + item.getEnd() + ">)";
            regions.add(value);
        });
        return regions;
    }


}
