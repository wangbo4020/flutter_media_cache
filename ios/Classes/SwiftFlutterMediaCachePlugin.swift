import Flutter
import UIKit

public class SwiftFlutterMediaCachePlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "plugins.auwx.cc/flutter_media_cache", binaryMessenger: registrar.messenger())
    let instance = SwiftFlutterMediaCachePlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    result("iOS " + UIDevice.current.systemVersion)
  }
}
