package io.jenkins.plugins.warnings.violations.github;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jeremymarshall on 30/12/18.
 */
public class Diff {
    private final String filename;
    private List<FromTo> fromTo;
    static private Pattern pattern = Pattern.compile("@@ \\-\\d+,\\d+ \\+(\\d+)(?:,(\\d+))? @@"
);

    public Diff(final String filename, final String diff) {
        this.filename = filename;
        this.fromTo = parse(diff);
    }

    private List<FromTo> parse(String diff) {
        Matcher matcher = pattern.matcher(diff);
        List<FromTo> ret = new ArrayList<>();
        while(matcher.find()) {
            int from = Integer.parseInt(matcher.group(1));
            String lenStr = matcher.group(2);
            int len = (lenStr==null ? 0 : Integer.parseInt(lenStr));
            ret.add(new FromTo( from, len));
        }
        return ret;
    }
}
