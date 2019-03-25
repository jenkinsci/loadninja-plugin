package com.smartbear.loadninja.plugins.jenkins;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.RelativePath;
import hudson.EnvVars;

import jenkins.model.*;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Date;
import java.text.*;
import java.sql.Timestamp;
import java.time.Instant;

import jenkins.tasks.SimpleBuildStep;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.*;

import com.smartbear.loadninja.api.SimpleAPI;
import com.smartbear.loadninja.helper.TestSummary;

public class LoadNinjaBuilder extends Builder implements SimpleBuildStep {

    private final String apiKey;
    private final String scenarioId;
    private String errorPassCriteria;
    private String durationPassCriteria;

    @DataBoundConstructor
    public LoadNinjaBuilder(String apiKey, String scenarioId, OptionalErrorBlock oeb, OptionalDurationBlock odb) {
        this.apiKey = apiKey;
        this.scenarioId = scenarioId;
        this.errorPassCriteria = (oeb != null) ? oeb.errorPassCriteria : null;
        this.durationPassCriteria = (odb != null) ? odb.durationPassCriteria : null;
    }

    public static class OptionalErrorBlock {
        private String errorPassCriteria;

        @DataBoundConstructor
        public OptionalErrorBlock(String errorPassCriteria) {
            this.errorPassCriteria = errorPassCriteria;
        }
    }

    public static class OptionalDurationBlock {
        private String durationPassCriteria;

        @DataBoundConstructor
        public OptionalDurationBlock(String durationPassCriteria) {
            this.durationPassCriteria = durationPassCriteria;
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public int getErrorPassCriteria() {
        return Integer.parseInt(errorPassCriteria);
    }

    public double getDurationPassCriteria() {
        return Double.parseDouble(durationPassCriteria);
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        Timestamp startTimestamp = new Timestamp(System.currentTimeMillis());

        long startTime = startTimestamp.getTime();


        LoadNinjaTestAction lt = new LoadNinjaTestAction(scenarioId);
        run.addAction(lt);

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        listener.getLogger().println("[" + df.format(new Date()) + "] Firing load test, from scenario: " + scenarioId + ", using APIKEY: "+apiKey+"!");

        // Get the Scenario

        String scenario = SimpleAPI.getScenarioFromId(apiKey, scenarioId);

        listener.getLogger().println("[" + df.format(new Date()) + "] Scenario got from scenarioId: " +scenario);

        // Fire the Test from the scenario ID
        sleep(10);
        String testId = SimpleAPI.runTest(apiKey, scenario);
        listener.getLogger().println("[" + df.format(new Date()) + "] Running test with testId:  " +testId);
        lt.setTestId(testId);
        String status = "";

        int sleepControl = 0;

        String ts = "";
        // Check for test Status and wait for it to finish... 
        while(!status.equals("TEST_COMPLETE")) {
            if(sleepControl % 60 == 0) {
                status = SimpleAPI.getTestStatus(apiKey, testId);
                listener.getLogger().println("[" + df.format(new Date()) + "] Check if the test is still running. Test status: " + status );
            }
            ts = SimpleAPI.getTestSummary(apiKey, testId);

            if (ts.equals("No test summary results available yet.")) {
                lt.setHasNoData(true);
            } else {
                listener.getLogger().println("[" + df.format(new Date()) + "] " + ts + "");
                lt.setHasNoData(false);
            }

            sleepControl += 5;
            sleep(5);
        }
        
        Timestamp endTimestamp = new Timestamp(System.currentTimeMillis());

        long endTime = endTimestamp.getTime();

        status = "";

        listener.getLogger().println("[" + df.format(new Date()) + "] Load test, " + testId + ", ended ok.");

        ts = SimpleAPI.getTestSummary(apiKey, testId);

        listener.getLogger().println("[" + df.format(new Date()) + "]" + ts + "");

        TestSummary fts = SimpleAPI.getFinalTestSummary(apiKey, testId);

        boolean passed = true;
        boolean errorFail = false;
        boolean durationFail = false;

        int numChecks = 0;

        if(fts.getTotalSteps() != -1 && fts.getPassedStepCount() != -1 && fts.getFailedStepCount() != -1 && fts.getAvgResponseTime() != -1) {
            if (errorPassCriteria != null) {
                errorFail = !(((float) fts.getFailedStepCount() / (float) fts.getTotalSteps() * 100) <= getErrorPassCriteria());
                if (errorFail) {
                    numChecks += 1;
                }
            }
    
            if (durationPassCriteria != null) {
                durationFail = !(fts.getAvgResponseTime() <= (getDurationPassCriteria() * 1000));
                if (durationFail) {
                    numChecks += 1;
                }
            }
        }

        Instant instant = startTimestamp.toInstant();

        String jUnit = "";
        jUnit += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        jUnit += "<testsuites name=\"LoadNinja Load Test (" + testId + ")\" tests=\"1\" failures=\""+ (numChecks > 0 ? 1 : 0) + "\" time=\""+ (endTime - startTime) +"\">\n";
        jUnit += "\t<testsuite name=\"LoadNinja Load Test (" + testId + ")\" tests=\"1\" failures=\""+ (numChecks > 0 ? 1 : 0) + "\" hostname=\"localhost\" time=\""+ (endTime - startTime) +"\" timestamp=\"" + instant + "\">\n";
        jUnit += "\t\t<testcase name=\"LoadNinja Load Test (" + testId + ")\">\n";

        passed = !errorFail && !durationFail;

        status = passed ? "Load Test Passed" : "Load Test Failed";

        lt.setStatus(status);

        if (!passed) {
            jUnit += "\t\t\t<failure message=\"";
            if (errorFail) {
                jUnit += "Percentage of errors (" + ((float) fts.getFailedStepCount() / (float) fts.getTotalSteps() * 100) + "%) has surpassed the error pass criteria of " + getErrorPassCriteria() + "%";
                listener.getLogger().println("[" + df.format(new Date()) + "] Load test, " + testId + ", has failed because percentage of errors (" + ((float) fts.getFailedStepCount() / (float) fts.getTotalSteps() * 100) + "%) has surpassed the error pass criteria of " + getErrorPassCriteria() + "%.");

                if (!durationFail) {
                    jUnit += ".";
                }
            }

            if (durationFail) {
                if(errorFail) {
                    jUnit += " and ";
                }

                jUnit += "Average step duration (" + fts.getAvgResponseTime() + "ms) has surpassed the duration pass criteria of " + (getDurationPassCriteria() * 1000) + "ms.";
                listener.getLogger().println("[" + df.format(new Date()) + "] Load test, " + testId + ", has failed because average step duration  (" + fts.getAvgResponseTime() + "ms) has surpassed the duration pass criteria of " + (getDurationPassCriteria() * 1000) + "ms.");
            }

            jUnit += " For more information, go to https://app.loadninja.com/test-results/" + testId + ".\"></failure>\n";
            jUnit += "\t\t</testcase>\n";
            jUnit += "\t</testsuite>\n";
            jUnit += "</testsuites>\n";

            FilePath fp = new FilePath(workspace, "results.xml");
            fp.write(jUnit, null);

            throw new IOException("[" + df.format(new Date()) + "] Load test, " + testId + " has failed to meet the criteria(s) set.");
        } else if (passed && fts.getTotalSteps() == -1 && fts.getPassedStepCount() == -1 && fts.getFailedStepCount() == -1 && fts.getAvgResponseTime() == -1) {
            listener.getLogger().println("[" + df.format(new Date()) + "] No test summary results or jUnit xml available. Test was terminated before any data was recorded.");
        }

        listener.getLogger().println("[" + df.format(new Date()) + "] Load test, " + testId + ", has passed.");

        jUnit += "\t\t</testcase>\n";
        jUnit += "\t</testsuite>\n";
        jUnit += "</testsuites>\n";

        FilePath fp = new FilePath(workspace, "results.xml");
        fp.write(jUnit, null);
    }

    private static void sleep(long ts) {
      try {
        Thread.sleep(ts*1000);
      } catch(Exception e) {

      }
    }
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public FormValidation doCheckErrorPassCriteria(@QueryParameter("errorPassCriteria") String errorPassCriteria)
                throws IOException, ServletException {
            if (errorPassCriteria.equals("") || Integer.parseInt(errorPassCriteria) < 0 || Integer.parseInt(errorPassCriteria) > 100 ) {
                FormValidation fv = FormValidation.error("Your pass criteria must be no less than 0 and no more than 100");
                return fv;
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckDurationPassCriteria(@QueryParameter("durationPassCriteria") String durationPassCriteria)
                throws IOException, ServletException {
            if (durationPassCriteria.equals("") || Double.parseDouble(durationPassCriteria) < 0 ) {
                return  FormValidation.error("Your pass criteria must be no less than 0");
            }

            String[] tokens = durationPassCriteria.split("\\.");

            if(tokens.length == 2 && tokens[1].length() > 2) {
                return  FormValidation.error("Your pass criteria must not have more than 2 digits after the decimal");
            }

            return FormValidation.ok();
        }

        @POST
        public FormValidation doValidateAPIKey(@QueryParameter("apiKey") final String apiKey) throws IOException, ServletException {
            Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
            try {
                boolean isValid = SimpleAPI.validateAPIKey(apiKey);
                return isValid ? FormValidation.ok("API Key is valid") : FormValidation.error("API Key is not valid");
            } catch (Exception e) {
                return FormValidation.error("Client error : "+e.getMessage());
            }
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.HelloWorldBuilder_DescriptorImpl_DisplayName();
        }

    }

}
