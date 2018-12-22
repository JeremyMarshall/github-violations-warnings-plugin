package io.jenkins.plugins.warnings.violations.github;

import hudson.Extension;
import hudson.PluginWrapper;
import hudson.model.*;
import io.jenkins.plugins.analysis.core.extension.warnings.Output;
import io.jenkins.plugins.analysis.core.extension.warnings.OutputDescriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Map;

/**
 * Created by jeremymarshall on 10/12/18.
 */
public class PullRequestComment extends Output {

    volatile String changeUrl;
    volatile String gitUrl;

    @DataBoundConstructor
    public PullRequestComment() {

    }

    @Override
    public boolean prepare(final Run<?, ?> run) {

        Map<String, String> env = System.getenv();

        // is it a PR
        if (env.containsKey("CHANGE_URL")) {
            changeUrl = env.get("CHANGE_URL");
            log("PR %s", changeUrl);
        } else {
            log("Not a PR");
            return false;
        }

        // see if its a git project
        if (env.containsKey("GIT_URL")) {
            gitUrl = env.get("GIT_URL");
            log("GIT  %s", gitUrl);
        } else {
            log("Not a git repo");
            return false;
        }

        // look through the GitHubServerConfig for a candidate for the api

        //build a i

        return true;
    }

    @Override
    public final void newIssuePrepare(final int size) {
        log("New");
    }

    @Override
    public final void newIssue(final String message, final String filename, final int lineStart){
        log("Issue '%s','%s','%d'", message, filename, lineStart);
    }

    @Override
    public final void outstandingIssuePrepare(final int size) {
        log("Outstanding");
    }

    @Override
    public final void outstandingIssue(final String message, final String filename, final int lineStart){
        log("Issue '%s','%s','%d'", message, filename, lineStart);
    }

    @Override
    public final void fixedIssuePrepare(final int size) {
        log("Fixed");
    }

    @Override
    public final void fixedIssue(final String message, final String filename, final int lineStart){
        log("Issue '%s','%s','%d'", message, filename, lineStart);
    }

    @Symbol("githubPullRequestComment")
    @Extension
    public static final class DescriptorImpl extends OutputDescriptor {
        @Override public String getDisplayName() {
            return "Github Pull Request Comment";
        }

    }

}
