package org.openhab.binding.openwms.internal;

public class Diverses {

    static int Long_little_endian_TO_big_endian(int i) {
        return ((i & 0xff) << 24) + ((i & 0xff00) << 8) + ((i & 0xff0000) >> 8) + ((i >> 24) & 0xff);

    }

    public Diverses() {

    }

    public static String stringToEndian(String t) {
        String s = t;
        int i = Integer.parseInt(s);
        int ii = Long_little_endian_TO_big_endian(i);
        s = Integer.toHexString(ii).toUpperCase();
        s = s.substring(0, 6);
        System.out.println(s);
        return s;
    }
}
