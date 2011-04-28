package com.ibdknox.socket_io_netty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SocketIOUtils {

	/**************************************************************************
	 * encode/decode
	 *************************************************************************/

	public static String encode(String msg) {
		int len = msg.length();
		return "~m~" + len + "~m~" + msg;
	}

	public static String decode(String msg) {
		Matcher regex = Pattern.compile("~m~[0-9]+~m~(.*)").matcher(msg);
		if (regex.matches())
			return regex.group(1);

		return msg;
	}
}
