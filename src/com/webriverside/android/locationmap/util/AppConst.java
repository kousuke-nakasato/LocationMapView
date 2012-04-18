package com.webriverside.android.locationmap.util;

public class AppConst {
	   private AppConst() {
	   }

	   //UI更新ハンドラーで、別スレッドからの処理を特定するための定数
	   public static final int MESSAGE_WHAT_SEARCHADDRESS_START = 11;
	   public static final int MESSAGE_WHAT_SEARCHADDRESS_END = 12;
	   public static final int MESSAGE_WHAT_SEARCHADDRESS_ERROR = -11;
	   public static final int MESSAGE_WHAT_LASTKNOWNPOINT_END = 1;
	   public static final int MESSAGE_WHAT_LASTKNOWNPOINT_ERROR = -1;
	   public static final int MESSAGE_WHAT_LASTKNOWNPOINT_TOADDRESS_ERROR = -2;
}