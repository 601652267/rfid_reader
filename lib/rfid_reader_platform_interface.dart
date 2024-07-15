import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'rfid_reader_method_channel.dart';

abstract class RfidReaderPlatform extends PlatformInterface {
  /// Constructs a RfidReaderPlatform.
  RfidReaderPlatform() : super(token: _token);

  static final Object _token = Object();

  static RfidReaderPlatform _instance = MethodChannelRfidReader();

  /// The default instance of [RfidReaderPlatform] to use.
  ///
  /// Defaults to [MethodChannelRfidReader].
  static RfidReaderPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [RfidReaderPlatform] when
  /// they register themselves.
  static set instance(RfidReaderPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<String?> init() async {
    throw UnimplementedError('init() has not been implemented.');
  }

  Future<void> setUHFPower(Map config) async {
    throw UnimplementedError('setUHFPower() has not been implemented.');
  }

  Future<void> changeType(Map config) async {
    throw UnimplementedError('changeType() has not been implemented.');
  }

  Future<void> setPowerLow() async {
    throw UnimplementedError('setPowerLow() has not been implemented.');
  }

  Future<void> setPowerHeight() async {
    throw UnimplementedError('setPowerHeight() has not been implemented.');
  }


  Future<void> useRFID() async {
    throw UnimplementedError('useRFID() has not been implemented.');
  }

  Future<void> useBLUE() async {
    throw UnimplementedError('useBLUE() has not been implemented.');
  }

  Future<void> userDefault() async {
    throw UnimplementedError('userDefault() has not been implemented.');
  }

  Future<void> onKeyUp() async {
    throw UnimplementedError('onKeyUp() has not been implemented.');
  }


  void setUpMethodCallHandler(Function resolve) {
    _instance.setUpMethodCallHandler(resolve);
  }
}
