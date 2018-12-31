package io.jenkins.plugins.warnings.violations.github;

/**
 * Created by jeremymarshall on 30/12/18.
 */
public class FromTo {
    private int from;
    private int to;

    public FromTo( final int from, final int len) {
        this.from = from;
        this.to = from + len;
    }
}
