
import 'rfid_reader_platform_interface.dart';
import 'dart:io' show Platform;

class RfidReader {

  // 是否能使用RFID功能
  bool couldUseRFID = false;

  Future<String?> getPlatformVersion() {
    return RfidReaderPlatform.instance.getPlatformVersion();
  }

  Future<bool> init() async  {
    if (Platform.isIOS) {
      couldUseRFID = false;
      return false;
    }
    String? resultStr = await RfidReaderPlatform.instance.init();
    if (resultStr == null || resultStr! == 'false') {
      couldUseRFID = false;
      return false;
    }
    return true;
  }

  void setUpMethodCallHandler(Function resolve) {
    RfidReaderPlatform.instance.setUpMethodCallHandler(resolve);
  }

  Future<void> setUHFPower(Map config) async {
    RfidReaderPlatform.instance.setUHFPower(config);
  }

  Future<void> changeType(Map config) async {
    RfidReaderPlatform.instance.changeType(config);
  }


}
