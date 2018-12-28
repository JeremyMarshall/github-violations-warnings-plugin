package io.jenkins.plugins.warnings.violations.github;

import hudson.Extension;
import hudson.model.*;
import hudson.util.LogTaskListener;
import io.jenkins.plugins.analysis.core.extension.warnings.Output;
import io.jenkins.plugins.analysis.core.extension.warnings.OutputDescriptor;
import org.jenkinsci.plugins.github.GitHubPlugin;
//import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
//import org.kohsuke.github.GitHub;
import org.jenkinsci.Symbol;
import org.kohsuke.github.*;
import org.kohsuke.stapler.DataBoundConstructor;
import com.cloudbees.jenkins.GitHubRepositoryName;
import java.io.IOException;
import java.util.*;
//import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import com.github.kostyasha.github.integration.generic.repoprovider.GHPermission;
import org.jenkinsci.plugins.github.util.misc.NullSafePredicate;
//import javax.annotation.Nonnull;
import com.google.common.base.Predicate;

import javax.annotation.Nonnull;

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

    @DataBoundConstructor
    public PullRequestComment() {

    }

    @Override
    public boolean prepare(final Run<?, ?> run) {

        Map<String, String> env = new HashMap<>();
        try {
            env = run.getEnvironment(new LogTaskListener(LOGGER, Level.INFO));
            env.forEach((k, v) -> log("key: " + k + " value:" + v));
        } catch (InterruptedException ex) {

        } catch (IOException ex) {

        }

        // is it a git project
        if (env.containsKey("CHANGE_URL")) {
            fromPR(env.get("CHANGE_URL"));
        } else {
            if (env.containsKey("GIT_URL")) {
                gitUrl = env.get("GIT_URL");
            } else {
                log("not a git repo");
                return false;
            }
            if (env.containsKey("GIT_BRANCH")) {
                gitBranch = env.get("GIT_BRANCH");
            } else {
                log("not a git repo");
                return false;
            }
        }

        GitHubRepositoryName repo = GitHubRepositoryName.create(gitUrl);

        Iterable<GHRepository> xxx = repo.resolve();

        for (GHRepository repository : xxx) {
            try {
                //pr = repository.getPullRequest(prID);
                Set<String> c = repository.getCollaboratorNames();
                if (c.contains(repo.getUserName())) {
                    myRepsitory = repository;
                    break;
                }
            } catch (IOException e) {
                log("user %s doesn't have access...", repo.getUserName());
            }
        }

        if(myRepsitory == null) {
            log("Didn't find a user with collaborator permissions to %s", gitUrl);
            return false;
        }

        if (prID == 0) {
            try {
                for(GHPullRequest pr: myRepsitory.getPullRequests(GHIssueState.OPEN)) {
                    String sss = pr.getHead().getRepository().getFullName();
                    if(gitUrl.contains(sss) && gitBranch.contains(pr.getHead().getRef())) {
                        prID = pr.getId();
                        break;
                    }
                }
            } catch (IOException e) {

            }
        }
        try {
            pr = myRepsitory.getPullRequest(prID);

            PagedIterable<GHPullRequestFileDetail> prf = pr.listFiles();
            prf.forEach((v) -> log("prf:" + v.getFilename() + " patch " +v.getPatch()));

//            PagedIterable<GHPullRequestReview> prc = pr.listReviews();
//            prc.forEach((v) -> log("prc:" + v.getCommitId()));
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

    private void fromPR(String prUrl) {
        Pattern pattern = Pattern.compile("(.*)/pull/(\\d+)$");

        // get a matcher object
        Matcher matcher = pattern.matcher(prUrl);
        if(matcher.find()) {
            gitUrl = matcher.group(1);
            prID = Integer.parseInt(matcher.group(2));
        }
    }

    static {
        LOGGER = Logger.getLogger(Run.class.getName());
    }
}