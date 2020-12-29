package com.auwx.flutter_media_cache

import android.util.Log
import androidx.annotation.NonNull
import com.danikula.videocache.CacheListener
import com.danikula.videocache.HttpProxyCacheServer
import com.danikula.videocache.file.Md5FileNameGenerator
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** FlutterMediaCachePlugin */
class FlutterMediaCachePlugin : FlutterPlugin, MethodCallHandler {
    companion object {
        private const val TAG = "MediaCachePlugin"
        private const val LOG = false
    }

    private lateinit var channel: MethodChannel
    private lateinit var stream: EventChannel
    private lateinit var server: HttpProxyCacheServer
    private lateinit var generation: MutableMap<String, String?>
    private val defaultGenerator = Md5FileNameGenerator()

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "plugins.auwx.cc/flutter_media_cache")
        channel.setMethodCallHandler(this)

        stream = EventChannel(flutterPluginBinding.binaryMessenger, "plugins.auwx.cc/flutter_media_cache_listener")
        stream.setStreamHandler(object : EventChannel.StreamHandler, CacheListener {

            private val cacheListenerSinks = mutableMapOf<String, EventChannel.EventSink>()
            override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
                if (LOG) Log.d(TAG, "onListen: $arguments")
                val url = arguments as String
                cacheListenerSinks[url] = events
                server.registerCacheListener(this, url)
            }

            override fun onCancel(arguments: Any?) {
                if (LOG) Log.d(TAG, "onCancel: $arguments")
                val url = arguments as String
                server.unregisterCacheListener(this)
                cacheListenerSinks.remove(url)?.endOfStream()
            }

            override fun onCacheAvailable(cacheFile: File?, url: String, percentsAvailable: Int) {
                if (LOG) Log.d(TAG, "onCacheAvailable: ${cacheFile?.name}, $percentsAvailable, $url ")
                cacheListenerSinks[url]?.success(percentsAvailable / 100.0)
                if (percentsAvailable >= 100) {
                    cacheListenerSinks.remove(url)?.endOfStream()
                }
            }
        })
        val context = flutterPluginBinding.applicationContext
        generation = mutableMapOf()
        server = HttpProxyCacheServer.Builder(context)
                .cacheDirectory(File(context.filesDir, "asset"))
                .fileNameGenerator { url ->
                    if (LOG) Log.d(TAG, "generate: $url map to ${generation[url]}")
                    return@fileNameGenerator generation[url] ?: defaultGenerator.generate(url)
                }
                .build()
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (LOG) Log.d(TAG, "onMethodCall: ${call.method}(${call.arguments})")
        if (call.method == "getProxyUrl") {
            val url = call.argument<String>("url")!!
            val name = call.argument<String>("file-name")

            generation[url] = name

            GlobalScope.launch {
//                val start = System.currentTimeMillis()
//                if (LOG) Log.d(TAG, "getProxyUrl: start")
                val proxyUrl = server.getProxyUrl(url)
//                if (LOG) Log.d(TAG, "getProxyUrl: end ${System.currentTimeMillis() - start}ms, $proxyUrl")
                withContext(Dispatchers.Main) {
                    result.success(proxyUrl)
                }
            }
        } else {
            result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        stream.setStreamHandler(null)
        channel.setMethodCallHandler(null)
        server.shutdown()
        generation.clear()
    }
}
