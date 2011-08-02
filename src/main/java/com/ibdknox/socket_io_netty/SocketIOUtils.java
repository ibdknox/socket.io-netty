package com.ibdknox.socket_io_netty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SocketIOUtils {

    /**************************************************************************
     * encode/decode
     *************************************************************************/

    private static final Pattern DECODE_PATTERN = Pattern.compile(
        "~m~[0-9]+~m~(.*)",
        Pattern.MULTILINE | Pattern.DOTALL
    );

    public static String encode(String msg) {
        int len = msg.length();
        return "~m~" + len + "~m~" + msg;
    }

    public static String decode(String msg) {
        Matcher regex = DECODE_PATTERN.matcher(msg);
        if (regex.matches())
            return regex.group(1);

        return msg;
    }
}
