package com.pdftron.demo.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonToken;
import com.pdftron.pdf.utils.Utils;

import java.util.Date;

/**
 * @hide
 * Created by Wesley Lin on 9/5/15.
 */

/**
 * modified slightly by PDFTron team
 * in the original package,
 * https://github.com/westlinkin/CacheUtilsLibrary
 * FileInputStream/FileOutputStream are not closed and therefore the resources are kept open
 * we also want to save/retrieve data in the same order (so using LinkedHashMap)
 */
public class CacheUtils {

    private static final String ENCODING = "utf8";
    private static final String FILE_SUFFIX = ".txt";
    public static String BASE_CACHE_PATH;
    public static final String CACHE_HEADER_LIST_OBJECT = "cache_header_list_object_v2";

    private static final String TAG = "CACHE_UTILS";

    public static void configureCache(Context context) {
        BASE_CACHE_PATH = context.getApplicationInfo().dataDir + File.separator + "files" + File.separator + "CacheUtils";

        if (new File(BASE_CACHE_PATH).mkdirs()) {
            Log.d(TAG, BASE_CACHE_PATH + " created.");
        }
    }

    private static String pathForCacheEntry(String name) {
        return BASE_CACHE_PATH + File.separator + name + FILE_SUFFIX;
    }

    private static <T> LinkedHashMap<String, T> dataMapsFromJson(String dataString) {
        if (TextUtils.isEmpty(dataString))
            return new LinkedHashMap<>();

        try {
            Type listType = new TypeToken<LinkedHashMap<String, T>>(){}.getType();
            return buildGson().fromJson(dataString, listType);
        } catch (Exception | Error e) {
            Log.d(TAG, "failed to read json" + e.toString());
            return new LinkedHashMap<>();
        }
    }

    private static <T> String dataMapstoJson(LinkedHashMap<String, T> dataMaps) {
        try {
            return buildGson().toJson(dataMaps);
        } catch (Exception | Error e) {
            Log.d(TAG, "failed to write json" + e.toString());
            return "[]";
        }
    }

    /**
     * @param fileName the name of the file
     * @return the content of the file, null if there is no such file
     */
    public static String readFile(String fileName) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(pathForCacheEntry(fileName));
            return IOUtils.toString(fis, ENCODING);
        } catch (IOException e) {
            Log.d(TAG, "read cache file failure" + e.toString());
            return null;
        } finally {
            Utils.closeQuietly(fis);
        }
    }

    /**
     * @param fileName the name of the file
     * @param fileContent the content of the file
     */
    public static void writeFile(String fileName, String fileContent) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(pathForCacheEntry(fileName));
            IOUtils.write(fileContent, fos, ENCODING);
        } catch (IOException e) {
            Log.d(TAG, "write cache file failure" + e.toString());
        } finally {
            Utils.closeQuietly(fos);
        }
    }

    /**
     * @param fileName the name of the file
     * @param dataMaps the map list you want to store
     */
    public static <T> void writeDataMapsFile(String fileName, LinkedHashMap<String, T> dataMaps) {
        writeFile(fileName, dataMapstoJson(dataMaps));
    }

    /**
     * @param fileName the name of the file
     * @return the map list you previous stored, an empty {@link List} will be returned if there is no such file
     */
    public static <T> LinkedHashMap<String, T> readDataMapsFile(String fileName) {
        return dataMapsFromJson(readFile(fileName));
    }

    private static <T> T objectFromJson(String dataString, Type t) {
        try {
            return buildGson().fromJson(dataString, t);
        } catch (Exception | Error e) {
            Log.e(TAG, "failed to read json" + e.toString());
            return null;
        }
    }

    private static <T> String objectToJson(T o) {
        try {
            return buildGson().toJson(o);
        } catch (Exception | Error e) {
            Log.e(TAG, "failed to write json" + e.toString());
            return null;
        }
    }

    /**
     * @param fileName the name of the file
     * @param object the object you want to store
     * @param <T> a class extends from {@link Object}
     */
    public static <T> void writeObjectFile(String fileName, T object) {
        writeFile(fileName, objectToJson(object));
    }

    /**
     * @param fileName the name of the file
     * @param t the type of the object you previous stored
     * @return the {@link T} type object you previous stored
     */
    public static <T> T readObjectFile(String fileName, Type t) {
        return objectFromJson(readFile(fileName), t);
    }

    private static <T> LinkedHashMap<String, T> dataMapFromJson(String dataString) {
        if (TextUtils.isEmpty(dataString))
            return new LinkedHashMap<>();

        try {
            Type t =  new TypeToken<LinkedHashMap<String, T>>(){}.getType();
            return buildGson().fromJson(dataString, t);
        } catch (Exception | Error e) {
            Log.e(TAG, "failed to read json" + e.toString());
            return new LinkedHashMap<>();
        }
    }

    private static <T> String dataMaptoJson(LinkedHashMap<String, T> dataMap) {
        try {
            return buildGson().toJson(dataMap);
        } catch (Exception | Error e) {
            Log.e(TAG, "failed to write json" + e.toString());
            return "{}";
        }
    }

    /**
     * @param fileName the name of the file
     * @param dataMap the map data you want to store
     */
    public static <T> void writeDataMapFile(String fileName, LinkedHashMap<String, T> dataMap) {
        writeFile(fileName, dataMaptoJson(dataMap));
    }

    /**
     * @param fileName the name of the file
     * @return the map data you previous stored
     */
    public static <T> LinkedHashMap<String, T> readDataMapFile(String fileName) {
        return dataMapFromJson(readFile(fileName));
    }

    /**
     * delete the file with fileName
     * @param fileName the name of the file
     */
    public static void deleteFile(String fileName) {
        FileUtils.deleteQuietly(new File(pathForCacheEntry(fileName)));
    }


    /**
     * check if there is a cache file with fileName
     * @param fileName the name of the file
     * @return true if the file exits, false otherwise
     */
    public static boolean hasCache(String fileName) {
        return new File(pathForCacheEntry(fileName)).exists();
    }

    static Gson buildGson() {
        GsonBuilder b = new GsonBuilder();
        b.registerTypeAdapter(Date.class, new TypeAdapter<Date>() {

            @Override
            public void write(com.google.gson.stream.JsonWriter writer, Date value) throws IOException {
                if (value == null) {
                    writer.nullValue();
                    return;
                }

                long num = value.getTime();
                num /= 1000;
                writer.value(num);
            }

            @Override
            public Date read(com.google.gson.stream.JsonReader reader) throws IOException {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    return null;
                }

                long value = reader.nextLong();
                return new Date(value * 1000);
            }

        });
        return b.create();
    }
}
