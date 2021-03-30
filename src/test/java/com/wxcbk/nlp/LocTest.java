package com.wxcbk.nlp;

import com.alibaba.fastjson.JSON;
import com.wxcbk.nlp.loc.data.LocationToken;
import com.wxcbk.nlp.loc.ner.CnLocRecognizer;

import java.util.List;
import java.util.Scanner;

/**
 * @author :owen
 * @Description :
 */
public class LocTest {

    public static void main(String[] args) {
        CnLocRecognizer cnLocRecognizer = new CnLocRecognizer();
        System.out.println("启动了");
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String str = scanner.nextLine();
            long start = System.currentTimeMillis();
            List<LocationToken> locTokens = cnLocRecognizer.locNer(null, str);
            System.out.println("耗时: " + (System.currentTimeMillis() - start) + "ms");
            System.out.println(JSON.toJSONString(locTokens));
        }

    }
}
