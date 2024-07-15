import 'dart:developer';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'rfid_reader_platform_interface.dart';

/// An implementation of [RfidReaderPlatform] that uses method channels.
class MethodChannelRfidReader extends RfidReaderPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('rfid_reader');

  final EventChannel _keyEventChannel = EventChannel('my_key_event_channel');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<String?> init() async {
    final res = await methodChannel.invokeMethod<String>('init');
    return res;
  }


  void setUpMethodCallHandler(Function resolve) {
    methodChannel.setMethodCallHandler((MethodCall call) async {});
    EventChannel('my_key_event_channel').receiveBroadcastStream().listen((event) {
      // 处理接收到的广播消息
      resolve(event.toString());
    });
  }

  Future<void> setUHFPower(Map config) async {
    await methodChannel.invokeMethod<String>('setUHFPower', config);
  }

  Future<void> setPowerLow() async {
    await methodChannel.invokeMethod<String>('setPowerLow', {});
  }

  Future<void> setPowerHeight() async {
    await methodChannel.invokeMethod<String>('setPowerHeight', {});
  }

  Future<void> changeType(Map config) async {
    await methodChannel.invokeMethod<String>('changeType', config);
  }



}
