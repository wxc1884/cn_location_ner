package com.wxcbk.nlp.loc.util;

import java.io.*;

/**
 * @author :owen
 * @Description :
 */
public class DeepCopyUtil {

    public static <T> T deepClone(T obj) {
        T deepObj = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            // 将流序列化成对象
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            deepObj = (T) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return deepObj;
    }
}
