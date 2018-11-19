package com.pdftron.android.pdfnetsdksamples

import android.app.job.JobInfo
import android.util.JsonWriter

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.UnsupportedEncodingException

/**
 * Created by Renchen on 6/23/2015.
 */
class DocxQAReportWriter @Throws(IOException::class)
constructor(private val mOutput: FileOutputStream?) {
    private val mWriter: JsonWriter

    init {
        mWriter = JsonWriter(OutputStreamWriter(mOutput, "UTF-8"))
        mWriter.setIndent("  ")
    }

    @Throws(IOException::class)
    fun BeginArray() {
        mWriter.beginArray()
    }

    @Throws(IOException::class)
    fun EndArray() {
        mWriter.endArray()
        mWriter.close()
    }

    @Throws(JSONException::class)
    private fun WriteJsonObject(jObj: JSONObject) {
        val keys = jObj.keys()
        while (keys.hasNext()) {
            val key = keys.next() as String
            if (jObj.get(key) is JSONObject) {
                try {
                    mWriter.name(key)
                    mWriter.beginObject()
                    WriteJsonObject(jObj.get(key) as JSONObject)
                    mWriter.endObject()
                } catch (e: IOException) {
                }

            } else if (jObj.get(key) is String) {
                try {
                    mWriter.name(key).value(jObj.get(key) as String)
                } catch (e: IOException) {
                }

            } else if (jObj.get(key) is Double) {
                try {
                    mWriter.name(key).value(jObj.get(key) as Double)
                } catch (e: IOException) {
                }

            } else if (jObj.get(key) is Number) {
                try {
                    mWriter.name(key).value(jObj.get(key) as Number)
                } catch (e: IOException) {
                }

            } else if (jObj.get(key) is Boolean) {
                try {
                    mWriter.name(key).value(jObj.get(key) as Boolean)
                } catch (e: IOException) {
                }

            } else if (jObj.get(key) is Long) {
                try {
                    mWriter.name(key).value(jObj.get(key) as Long)
                } catch (e: IOException) {
                }

            }
        }
    }

    @Throws(IOException::class)
    fun WriteDocumentObject(docObj: DocumentObject) {
        mWriter.beginObject()
        mWriter.name("FileName").value(docObj.getmName())
        mWriter.name("TotalTime").value(docObj.getmTotalConvertTime())
        mWriter.name("NumPages").value(docObj.getmNumPages().toLong())

        if (docObj.getmTimeStats() != null) {
            try {
                mWriter.name("TimeStats")
                mWriter.beginObject()
                val jObj = JSONObject(docObj.getmTimeStats())
                WriteJsonObject(jObj)
                mWriter.endObject()
            } catch (e: JSONException) {
            }

        }
        if (docObj.getmWarnings() != null) {
            try {
                mWriter.name("Warnings")
                mWriter.beginArray()
                val jArray = JSONArray(docObj.getmWarnings())
                for (i in 0 until jArray.length()) {
                    mWriter.beginObject()
                    val jObj = jArray.getJSONObject(i)
                    val msg = jObj.getString("msg")
                    val file = jObj.getString("file")
                    val line = jObj.getInt("line")
                    val code = jObj.getInt("code")
                    mWriter.name("msg").value(msg)
                    mWriter.name("file").value(file)
                    mWriter.name("line").value(line.toLong())
                    mWriter.name("code").value(code.toLong())
                    mWriter.endObject()
                }
                mWriter.endArray()
            } catch (e: JSONException) {
            }

        }

        if (docObj.getmErrors() != null) {
            mWriter.name("Errors").value(docObj.getmErrors())
        }

        mWriter.endObject()
    }
}
