package com.wxcbk.nlp.loc.ner;

import com.wxcbk.nlp.loc.data.Region;
import com.wxcbk.nlp.loc.data.RegionPointAndEdge;
import com.wxcbk.nlp.loc.util.DeepCopyUtil;
import com.wxcbk.nlp.loc.util.LocFileUtil;
import com.wxcbk.nlp.loc.ptct.PatriciaTrie;
import javafx.util.Pair;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author :owen
 * @Description :
 */
public class LocRegionTrie {

    private static final Logger log = LoggerFactory.getLogger(CnLocRecognizer.class);

    /**
     * 字典树存储地名与该地名的节点信息
     */
    private PatriciaTrie<RegionPointAndEdge> patriciaTrie;
    /**
     * <行政区域编号,行政区域关联信息> 如<110000, {北京..region}>
     */
    private Map<String, Region> regionMap;


    public void buildTrie() {
        long startTime = System.currentTimeMillis();
        this.patriciaTrie = new PatriciaTrie<>();
        this.regionMap = LocFileUtil.readFileToLocRegion();
        List<Pair<String, RegionPointAndEdge>> nodeItems = regionToRegionPoint(regionMap);
        this.patriciaTrie.buildPatriciaTrie(nodeItems);
        log.info("CnLocRecognizer build time:{}s", (System.currentTimeMillis() - startTime) / 1000.0);
    }

    public void addLocNicknameToTrie(String filePath) {

    }

    public void addLocNicknameToTrie(Map<String, List<String>> normAndSynWords, Map<String, Region> regionMap) {
        LocFileUtil.addLocNicknameToTrie(normAndSynWords, regionMap);
    }


    private List<Pair<String, RegionPointAndEdge>> regionToRegionPoint(Map<String, Region> regionMap) {
        List<Pair<String, RegionPointAndEdge>> wordAndRegions = new ArrayList<>();
        regionMap.forEach((locCode, region) -> {
            RegionPointAndEdge regionPointAndEdge = region.getOfficialRegion();
            String shortName = region.getShortName();
            RegionPointAndEdge normRegionPoint = DeepCopyUtil.deepClone(regionPointAndEdge);
            normRegionPoint.setValue(shortName);
            wordAndRegions.add(new Pair<>(shortName, normRegionPoint));
            if (CollectionUtils.isNotEmpty(region.getNicknames())) {
                region.getNicknames().forEach(name -> {
                    RegionPointAndEdge nicknameRegionPoint = DeepCopyUtil.deepClone(regionPointAndEdge);
                    nicknameRegionPoint.setValue(name);
                    wordAndRegions.add(new Pair<>(name, nicknameRegionPoint));
                });
            }
        });
        return wordAndRegions;
    }


    public PatriciaTrie<RegionPointAndEdge> getPatriciaTrie() {
        return patriciaTrie;
    }

    public void setPatriciaTrie(PatriciaTrie<RegionPointAndEdge> patriciaTrie) {
        this.patriciaTrie = patriciaTrie;
    }

    public Map<String, Region> getRegionMap() {
        return regionMap;
    }

    public void setRegionMap(Map<String, Region> regionMap) {
        this.regionMap = regionMap;
    }
}
