package com.smartbear.loadninja.plugins.jenkins;

import hudson.model.Run;
import jenkins.model.RunAction2;

public class LoadNinjaTestAction implements  RunAction2 {
    private String scenarioId = "";
    private String testId = "";
    private String status = "Still Running Test";
    private boolean hasNoData = true;

    private transient Run run;

    @Override
    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.run = run;
    }

    public Run getRun() {
        return run;
    }

    public LoadNinjaTestAction(String scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getScenarioId() {
        return scenarioId;
    }


    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean getHasNoData() {
        return hasNoData;
    }

    public void setHasNoData(boolean hasNoData) {
        this.hasNoData = hasNoData;
    }
    
    @Override
    public String getIconFileName() {
        return "document.png";
    }

    @Override
    public String getDisplayName() {
        return "LoadNinja";
    }

    @Override
    public String getUrlName() {
        return "loadninja";
    }
}
