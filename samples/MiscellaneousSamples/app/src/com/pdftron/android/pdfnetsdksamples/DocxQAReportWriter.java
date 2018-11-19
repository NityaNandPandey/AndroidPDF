package com.pdftron.android.pdfnetsdksamples;
import android.app.job.JobInfo;
import android.util.JsonWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

/**
 * Created by Renchen on 6/23/2015.
 */
public class DocxQAReportWriter {
    private FileOutputStream mOutput;
    private JsonWriter mWriter;
    public DocxQAReportWriter(FileOutputStream outputStream) throws IOException
    {
       mOutput = outputStream;
       mWriter = new JsonWriter(new OutputStreamWriter(mOutput, "UTF-8"));
       mWriter.setIndent("  ");
    }

    public void BeginArray() throws IOException
    {
        mWriter.beginArray();
    }

    public void EndArray() throws IOException
    {
        mWriter.endArray();
        mWriter.close();
    }

    private void WriteJsonObject(JSONObject jObj) throws JSONException
    {
        Iterator<?> keys = jObj.keys();
        while (keys.hasNext())
        {
            String key = (String)keys.next();
            if (jObj.get(key) instanceof JSONObject)
            {
                try {
                    mWriter.name(key);
                    mWriter.beginObject();
                    WriteJsonObject((JSONObject)jObj.get(key));
                    mWriter.endObject();
                }
                catch (IOException e){}
            }
            else if (jObj.get(key) instanceof String)
            {
                try {
                    mWriter.name(key).value((String) jObj.get(key));
                }
                catch(IOException e) {}
            }
            else if (jObj.get(key) instanceof Double)
            {
                try{
                    mWriter.name(key).value((Double) jObj.get(key));
                }
                catch (IOException e){}
            }
            else if (jObj.get(key) instanceof Number)
            {
                try{
                    mWriter.name(key).value((Number) jObj.get(key));
                }
                catch (IOException e){}
            }
            else if (jObj.get(key) instanceof Boolean)
            {
                try{
                    mWriter.name(key).value((Boolean)jObj.get(key));
                }
                catch(IOException e){}
            }
            else if (jObj.get(key) instanceof Long)
            {
                try{
                    mWriter.name(key).value((Long) jObj.get(key));
                }
                catch(IOException e){}
            }
        }
    }

    public void WriteDocumentObject(DocumentObject docObj) throws IOException
    {
        mWriter.beginObject();
        mWriter.name("FileName").value(docObj.getmName());
        mWriter.name("TotalTime").value(docObj.getmTotalConvertTime());
        mWriter.name("NumPages").value(docObj.getmNumPages());

        if (docObj.getmTimeStats() != null)
        {
            try{
                mWriter.name("TimeStats");
                mWriter.beginObject();
                JSONObject jObj = new JSONObject(docObj.getmTimeStats());
                WriteJsonObject(jObj);
                mWriter.endObject();
            }
            catch (JSONException e){}
        }
        if (docObj.getmWarnings() != null)
        {
            try {
                mWriter.name("Warnings");
                mWriter.beginArray();
                JSONArray jArray = new JSONArray(docObj.getmWarnings());
                for (int i = 0; i < jArray.length(); i++)
                {
                    mWriter.beginObject();
                    JSONObject jObj = jArray.getJSONObject(i);
                    String msg = jObj.getString("msg");
                    String file = jObj.getString("file");
                    int line = jObj.getInt("line");
                    int code = jObj.getInt("code");
                    mWriter.name("msg").value(msg);
                    mWriter.name("file").value(file);
                    mWriter.name("line").value(line);
                    mWriter.name("code").value(code);
                    mWriter.endObject();
                }
                mWriter.endArray();
            }
            catch (JSONException e){}
        }

        if (docObj.getmErrors() != null)
        {
            mWriter.name("Errors").value(docObj.getmErrors());
        }

        mWriter.endObject();
    }
}
