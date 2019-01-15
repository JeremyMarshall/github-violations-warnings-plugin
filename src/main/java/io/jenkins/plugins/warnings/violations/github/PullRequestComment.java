package io.jenkins.plugins.warnings.violations.github;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.thoughtworks.xstream.mapper.Mapper;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.*;
import hudson.plugins.git.GitSCM;
import hudson.scm.NullSCM;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SCM;
import hudson.slaves.NodeProperty;
import hudson.util.LogTaskListener;
import io.jenkins.plugins.analysis.core.extension.warnings.Output;
import io.jenkins.plugins.analysis.core.extension.warnings.OutputDescriptor;
import io.jenkins.plugins.analysis.core.extension.warnings.OutputRunCache;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.github.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jeremymarshall on 10/12/18.
 */
public class PullRequestComment extends Output {
    private static final Logger LOGGER;
    private static Pattern pattern = Pattern.compile("(.*)/pull/(\\d+)$");

    @DataBoundConstructor
    public PullRequestComment() {

    }

    @Override
    public OutputRunCache prepare(final Run<?, ?> run) {

        try {
            return new PullRequestCommentRunCache(run);
        } catch (PRCException ex) {
            log(ex.getMessage());
            return null;
        }
    }

    @Override
    public final void newIssuePrepare(final int size, OutputRunCache cache) {
        log("New");
    }

    @Override
    public final void newIssue(OutputRunCache cache, final String message, final String filename, final int lineStart) {
        if (isInPR(cache, filename, lineStart))
            log("Issue '%s','%s','%d'", message, filename, lineStart);
    }

    @Override
    public final void outstandingIssuePrepare(final int size, OutputRunCache cache) {
        log("Outstanding");
    }

    @Override
    public final void outstandingIssue(OutputRunCache cache, final String message, final String filename, final int lineStart) {
        if (isInPR(cache, filename, lineStart))
            log("Issue '%s','%s','%d'", message, filename, lineStart);
    }

    @Override
    public final void fixedIssuePrepare(final int size, OutputRunCache cache) {
        log("Fixed");
    }

    @Override
    public final void fixedIssue(OutputRunCache cache, final String message, final String filename, final int lineStart) {
        if (isInPR(cache, filename, lineStart))
            log("Issue '%s','%s','%d'", message, filename, lineStart);
    }

    @Symbol("githubPullRequestComment")
    @Extension
    public static final class DescriptorImpl extends OutputDescriptor {
        @Override
        public String getDisplayName() {
            return "Github Pull Request Comment";
        }

    }

    private static boolean isInPR(OutputRunCache cache, final String filename, final int lineStart) {

        PullRequestCommentRunCache prcrc = (PullRequestCommentRunCache) cache;

        Predicate<FromTo> xx = ft -> {
            return lineStart >= ft.getFrom() && lineStart <= ft.getTo();
        };


        return prcrc.inPR(filename, xx);
    }

    static {
        LOGGER = Logger.getLogger(Run.class.getName());
    }
}