package com.bsuyeon.mlkitinterlock

import android.content.Context
import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.FileNotFoundException
import java.io.InputStream

class DaeLoader(context: Context) {
    private lateinit var inputStream: InputStream
    private lateinit var xmlParser: XmlPullParser

    init {
        try {
            val assetManager = context.resources.assets;
            inputStream = assetManager.open("xbot.dae")

            val xmlFactory = XmlPullParserFactory.newInstance()
            xmlParser = xmlFactory.newPullParser()
            xmlParser.setInput(inputStream, null)
        } catch (e: FileNotFoundException) {
            Log.e("FBXLoader", "file not found ${e.message}")
        }
    }

    private fun load() {
        var event = xmlParser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            Log.d(TAG, "depth 0 ${xmlParser.name}")
            if (xmlParser.depth > 0) {
                Log.d(TAG, "depth 1 ${xmlParser.name}")
            }
            event = xmlParser.next()
        }
        inputStream.close()
    }

    companion object {
        private const val TAG = "FBXLoader"
    }
}