package ee.ut.physics.digi.tbd.robot.util;

import java.util.Arrays;

public final class StringUtil {

    private StringUtil() {}

    public static String padRight(String text, char paddingChar, int length) {
        int paddingSize = length - text.length();
        char[] padding = new char[paddingSize];
        Arrays.fill(padding, paddingChar);
        return text + new String(padding);
    }

}
