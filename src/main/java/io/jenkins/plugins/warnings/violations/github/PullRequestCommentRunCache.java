package io.jenkins.plugins.warnings.violations.github;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.util.LogTaskListener;
import io.jenkins.plugins.analysis.core.extension.warnings.OutputRunCache;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.github.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jeremymarshall on 4/1/19.
 */
public class PullRequestCommentRunCache extends OutputRunCache {
    private static final Logger LOGGER;
    private static Pattern pattern = Pattern.compile("(.*)/pull/(\\d+)$");

    private String gitUrl = null;
    private String gitBranch;
    int prID = 0;
    private GHRepository myRepsitory;
    private GHPullRequest pr = null;
    private List<Diff> diff = new ArrayList<>();

    public PullRequestCommentRunCache(final Run<?, ?> run) throws PRCException{

        Map<String, String> env;

        try {
            env = run.getEnvironment(new LogTaskListener(LOGGER, Level.INFO));
        } catch (InterruptedException | IOException ex) {
            throw new PRCException("couldn't get environment");
        }

        // is it a git project
        Matcher matcher = pattern.matcher(env.getOrDefault("CHANGE_URL", ""));
        if(matcher.find()) {
            gitUrl = matcher.group(1);
            prID = Integer.parseInt(matcher.group(2));
        } else {
            Job<?, ?> job = run.getParent();
            SCM scm = null;
            GitSCM gitScm = null;

            if (job instanceof WorkflowJob) {
                Collection<? extends SCM> scms = ((WorkflowJob) job).getSCMs();
                if (!scms.isEmpty()) {
                    scm = scms.iterator().next(); // TODO: what should we do if more than one SCM has been used
                }
            }
            else if (run instanceof AbstractBuild) {
                AbstractProject project = ((AbstractBuild) run).getProject();
                if (project.getScm() != null) {
                    scm = project.getScm();
                }
                scm = project.getRootProject().getScm();
            }
            if(scm instanceof GitSCM) {
                gitScm = (GitSCM) scm;

                gitUrl = gitScm.getUserRemoteConfigs().get(0).getUrl();
                gitBranch = gitScm.getBranches().get(0).getName();
            } else {
                throw new PRCException("not a git repo");
            }
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
            throw new PRCException("Didn't find a user with collaborator permissions to %s", gitUrl);
        }

        if (prID == 0) {

            Predicate<GHPullRequest> prForBranch = pr1 -> {
                String sss = pr1.getHead().getRepository().getFullName();
                if(gitUrl.contains(sss) && gitBranch.contains(pr1.getHead().getRef())) {
                    return true;
                }
                return false;
            };

            try {
                pr = Iterables.tryFind(myRepsitory.getPullRequests(GHIssueState.OPEN), prForBranch).orNull();
            } catch (IOException e) {
                throw new PRCException(e.getMessage());
            }

            if(pr == null) {
                throw new PRCException("No PR found");
            }
            prID = pr.getNumber();

        }
        try {
            pr = myRepsitory.getPullRequest(prID);

            PagedIterable<GHPullRequestFileDetail> prf = pr.listFiles();

            pr.listFiles().forEach((v) -> diff.add(new Diff( v.getFilename(), v.getPatch())));
        } catch (IOException e) {
            throw new PRCException(e.getMessage());        }
    }

    static {
        LOGGER = Logger.getLogger(Run.class.getName());
    }
}
