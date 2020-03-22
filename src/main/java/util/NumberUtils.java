package util;

public class NumberUtils {

    public static int parseInt(String possibleInt, int defaultValue) {
        try {
            return Integer.parseInt(possibleInt);
        }
        catch (Exception e) {
            return defaultValue;
        }
    }
}
