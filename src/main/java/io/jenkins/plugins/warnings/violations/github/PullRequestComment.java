package io.jenkins.plugins.warnings.violations.github;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.thoughtworks.xstream.mapper.Mapper;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.slaves.NodeProperty;
import hudson.util.LogTaskListener;
import io.jenkins.plugins.analysis.core.extension.warnings.Output;
import io.jenkins.plugins.analysis.core.extension.warnings.OutputDescriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.github.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jeremymarshall on 10/12/18.
 */
public class PullRequestComment extends Output {
    private static final Logger LOGGER;
    volatile private String gitUrl = "";
    volatile private String gitBranch = "";
    volatile private int prID = 0;
    volatile private GHRepository myRepsitory = null;
    volatile private GHPullRequest pr = null;
    volatile private List<Diff> diff = new ArrayList<>();

    @DataBoundConstructor
    public PullRequestComment() {

    }

    @Override
    public boolean prepare(final Run<?, ?> run) {

        Map<String, String> env = null;
        try {
            env = run.getEnvironment(new LogTaskListener(LOGGER, Level.INFO));
            env.forEach((k, v) -> log("key: " + k + " value:" + v));
        } catch (InterruptedException | IOException ex) {
            log("couldn't get environment");
            return false;
        }

        // is it a git project
        if (! fromEnv(env.getOrDefault("CHANGE_URL", ""), env)) {
            log("not a git repo");
            return false;
        }

        GitHubRepositoryName repo = GitHubRepositoryName.create(gitUrl);

        Predicate<GHRepository> canPush = repository -> {
            try {
                repository.getCollaboratorNames();
                return true;
            } catch (IOException e) {
                return false;
            }
        };

        myRepsitory = Iterables.tryFind(repo.resolve(), canPush).orNull();

        if(myRepsitory == null) {
            log("Didn't find a user with collaborator permissions to %s", gitUrl);
            return false;
        }

        if (prID == 0) {

            Predicate<GHPullRequest> prForBranch = pr -> {
                String sss = pr.getHead().getRepository().getFullName();
                if(gitUrl.contains(sss) && gitBranch.contains(pr.getHead().getRef())) {
                    return true;
                }
                return false;
            };

            try {
                pr = Iterables.tryFind(myRepsitory.getPullRequests(GHIssueState.OPEN), prForBranch).orNull();
            } catch (IOException e) {

            }
            if(pr != null) {
                prID = pr.getNumber();
            }
        }
        try {
            pr = myRepsitory.getPullRequest(prID);

            PagedIterable<GHPullRequestFileDetail> prf = pr.listFiles();

            pr.listFiles().forEach((v) -> diff.add(new Diff( v.getFilename(), v.getPatch())));

            prf.forEach((v) -> log("prf:" + v.getFilename() + " patch " +v.getPatch()));
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    @Override
    public final void newIssuePrepare(final int size) {
        log("New");
    }

    @Override
    public final void newIssue(final String message, final String filename, final int lineStart) {
        log("Issue '%s','%s','%d'", message, filename, lineStart);
    }

    @Override
    public final void outstandingIssuePrepare(final int size) {
        log("Outstanding");
    }

    @Override
    public final void outstandingIssue(final String message, final String filename, final int lineStart) {
        log("Issue '%s','%s','%d'", message, filename, lineStart);
    }

    @Override
    public final void fixedIssuePrepare(final int size) {
        log("Fixed");
    }

    @Override
    public final void fixedIssue(final String message, final String filename, final int lineStart) {
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

    private boolean fromEnv(String prUrl, Map<String, String> env) {
        Pattern pattern = Pattern.compile("(.*)/pull/(\\d+)$");

        // get a matcher object
        Matcher matcher = pattern.matcher(prUrl);
        if(matcher.find()) {
            gitUrl = matcher.group(1);
            prID = Integer.parseInt(matcher.group(2));
            return true;
        } else {
            gitUrl = env.getOrDefault("GIT_URL", null);
            gitBranch = env.getOrDefault("GIT_BRANCH", null);
            return gitUrl != null && gitBranch != null;
        }
    }

    static {
        LOGGER = Logger.getLogger(Run.class.getName());
    }
}