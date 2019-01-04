package io.jenkins.plugins.warnings.violations.github;

/**
 * Created by jeremymarshall on 4/1/19.
 */
public class PRCException extends Exception {
    public PRCException(String ex) {
        super(ex);
    }
    public PRCException(String ex, String parms) {
        super(String.format(ex, parms));
    }
}
