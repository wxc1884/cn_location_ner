package com.wxcbk.nlp.loc.util;

import com.csvreader.CsvReader;
import com.wxcbk.nlp.loc.data.Region;
import com.wxcbk.nlp.loc.data.RegionPointAndEdge;
import com.wxcbk.nlp.loc.ner.CnLocRecognizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author :owen
 * @Description :
 */
public class LocFileUtil {
    private static final Logger log = LoggerFactory.getLogger(CnLocRecognizer.class);
    private static String areaDataPath = "/data/area.csv";
    private static String areaAliasDataPath = "/data/area_alias.csv";

    public static Map<String, Region> readFileToLocRegion() {
        Map<String, Region> areaMap = new HashMap<>();
        Map<String, List<String>> aliasMap = new HashMap<>();
        Map<String, Integer> locCode2num = new HashMap<>();
        InputStream is = LocFileUtil.class.getResourceAsStream(areaDataPath);
        CsvReader csvReader = new CsvReader(is, ',', StandardCharsets.UTF_8);
        InputStream is1 = LocFileUtil.class.getResourceAsStream(areaAliasDataPath);
        CsvReader csvReader1 = new CsvReader(is1, ',', StandardCharsets.UTF_8);
        int i = 0;
        try {
            csvReader.readHeaders();
            while (csvReader.readRecord()) {
                String[] rawLines = csvReader.getValues();
                i++;
                putRawLineToAreaMap(rawLines, areaMap, locCode2num);
            }
            log.info("[loc_ner] loc data line num={}", i);
            completePreLocCode(areaMap);
            csvReader1.readHeaders();
            while (csvReader1.readRecord()) {
                String[] rawLine1 = csvReader1.getValues();
                putRawLineToAliasMap(rawLine1, aliasMap);
            }
            completeRegionAlias(areaMap, aliasMap);
        } catch (Exception e) {
            log.error("[loc_ner] read area data error:", e);
        }
        log.info("[loc_ner] areaMap size={}", areaMap.size());
        return areaMap;
    }

    public static void addLocNicknameToTrie(Map<String, List<String>> normAndSynWords, Map<String, Region> regionMap) {
        completeRegionAlias(regionMap, normAndSynWords);
    }

    private static void completeRegionAlias(Map<String, Region> areaMap, Map<String, List<String>> aliasMap) {
        aliasMap.forEach((locCode, alias) -> {
            Region region = areaMap.get(locCode);
            region.setNicknames(alias);
        });
    }

    private static void putRawLineToAliasMap(String[] rawLine, Map<String, List<String>> aliasMap) {
        String locCode = rawLine[0];
        String level = rawLine[1];
        String alia = rawLine[2];
        String[] alias = alia.split("\\|");
        List<String> aliaList = new ArrayList<>(Arrays.asList(alias));
        aliasMap.put(level + "_" + locCode, aliaList);
    }

    private static void completePreLocCode(Map<String, Region> areaMap) {
        areaMap.forEach((locCode, region) -> {
            if (region.getLevel() != 2) {
                putPreLevelToOfficialRegion(region.getOfficialRegion(), region.getLevel(), areaMap);
            }
        });
    }


    private static void putPreLevelToOfficialRegion(RegionPointAndEdge regionPoint, int level, Map<String, Region> areaMap) {
        switch (level) {
            case 5:
                String preLevel3LocCode = areaMap.get(regionPoint.getPreLevel4()).getOfficialRegion().getPreLevel3();
                regionPoint.setPreLevel3(preLevel3LocCode);
                String preLevel2LocCode = areaMap.get(preLevel3LocCode).getOfficialRegion().getPreLevel2();
                regionPoint.setPreLevel2(preLevel2LocCode);
                break;
            case 4:
                String preLevel2LocCode1 = areaMap.get(regionPoint.getPreLevel3()).getOfficialRegion().getPreLevel2();
                regionPoint.setPreLevel2(preLevel2LocCode1);
                break;
            default:
        }

    }

    private static void putRawLineToAreaMap(String[] rawLines, Map<String, Region> areaMap, Map<String, Integer> locCode2num) {
        String locCode = rawLines[0];
        String locName = rawLines[1];
        String locLevel = rawLines[3];
        String preLocCode = rawLines[4];
        String shortName = rawLines[5];
        String suffixName = "";
        if (rawLines.length == 7) {
            suffixName = rawLines[6];
        }
        locCode = locLevel + "_" + locCode;
        if (areaMap.containsKey(locCode) && "5".equals(locLevel)) {
            if (locCode2num.containsKey(locCode)) {
                int num = locCode2num.get(locCode) + 1;
                locCode2num.put(locCode, num);
                locCode = locCode + num;
            } else {
                locCode2num.put(locCode, 0);
                locCode = locCode + 0;
            }
        }
        Region region = new Region();
        RegionPointAndEdge regionPointAndEdge = new RegionPointAndEdge();
        regionPointAndEdge.setLevel(Integer.parseInt(locLevel));
        regionPointAndEdge.setLocCode(locCode);
        regionPointAndEdge.setPreLocCode(preLocCode);
        region.setLevel(Integer.parseInt(locLevel));
        region.setOfficialRegion(regionPointAndEdge);
        region.setNormValue(locName);
        region.setShortName(shortName);
        region.setSuffixName(suffixName);
        areaMap.put(locCode, region);
    }

    public static void main(String[] args) {
        readFileToLocRegion();
    }


}
