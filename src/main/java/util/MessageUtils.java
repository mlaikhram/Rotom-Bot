package util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {

    public static String ID_FORMAT = "<@%s>";
    public static String ID_REGEX = "<@[0-9]+>";

    public static Long mentionToUserID(String mention) {
        return Long.parseLong(mention.replaceAll("[<@!>]", ""));
    }

    public static String userIDToMention(String id) {
        return String.format(ID_FORMAT, id);
    }

    public static boolean isUserMention(String mention) {
        return mention.replaceAll("!", "").matches(ID_REGEX);
    }

    public static Collection<Long> getMentionsFromText(String text) {
        Collection<Long> matches = new ArrayList<>();
        Matcher m = Pattern.compile(ID_REGEX).matcher(text.replaceAll("!", ""));
        while (m.find()) {
            matches.add(mentionToUserID(m.group()));
        }
        return matches;
    }
}
