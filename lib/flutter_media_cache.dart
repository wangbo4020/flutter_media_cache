import 'dart:async';

import 'package:flutter/services.dart';

class FlutterMediaCache {
  static const _channel =
      const MethodChannel('plugins.auwx.cc/flutter_media_cache');
  static const _event =
      const EventChannel('plugins.auwx.cc/flutter_media_cache_listener');

  /// 缓存视频
  ///
  /// [url] 需要缓存的视频链接
  ///
  /// [name] 缓存后本地文件
  static Future<String> getProxyUrl(String url, {String? name}) {
    return _channel.invokeMethod('getProxyUrl', {
      'url': url,
      "file-name": name,
    }).then((v) => v);
  }

  /// 监听缓存进度
  static Stream<double> receiveProgress(String url) {
    return _event.receiveBroadcastStream(url).map<double>((event) => event);
  }
}
