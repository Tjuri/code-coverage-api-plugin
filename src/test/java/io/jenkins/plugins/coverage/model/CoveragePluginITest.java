package io.jenkins.plugins.coverage.model;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.HealthReport;
import hudson.model.Result;
import hudson.model.Run;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.source.DefaultSourceFileResolver;
import io.jenkins.plugins.coverage.source.SourceFileResolver;
import io.jenkins.plugins.coverage.source.SourceFileResolver.SourceFileResolverLevel;
import io.jenkins.plugins.coverage.threshold.Threshold;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the coverage API plugin.
 *
 * @author Ullrich Hafner
 */
public class CoveragePluginITest extends IntegrationTestWithJenkinsPerSuite {

    // TODO: other possibility than duplicating files because of different ressource folder ?
    // TODO: Difference between **/*.xml and *.xml. Make consistent
    private static final String JACOCO_ANALYSIS_MODEL_FILE_NAME = "jacoco-analysis-model.xml";
    private static final String JACOCO_CODING_STYLE_FILE_NAME = "jacoco-codingstyle.xml";
    private static final String JACOCO_CODING_STYLE_DECREASED_FILE_NAME = "jacoco-codingstyle-decreased-line-coverage.xml";
    private static final String COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME = "coverage-with-lots-of-data.xml";
    private static final String COBERTURA_COVERAGE_FILE_NAME = "cobertura-coverage.xml";

    private static final int JACOCO_ANALYSIS_MODEL_LINES_TOTAL = 6368;
    private static final int JACOCO_ANALYSIS_MODEL_LINES_COVERED = 6083;
    private static final int JACOCO_CODING_STYLE_LINES_TOTAL = 323;
    private static final int JACOCO_CODING_STYLE_LINES_COVERED = 294;
    private static final int COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_COVERED = 602;
    private static final int COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_TOTAL = 958;

    private static final int COBERTURA_COVERAGE_LINES_COVERED = 2;
    private static final int COBERTURA_COVERAGE_LINES_TOTAL = 2;

    /** Test with no adapters */
    @Test
    public void freestyleWithEmptyAdapters() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setAdapters(Collections.emptyList());
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult).isEqualTo(null);
    }

    /** Test with JacocoAdapter and no files */
    @Test
    public void freestyleJacocoWithEmptyFiles() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult).isEqualTo(null);
    }

    /** Test with one Jacoco file */
    @Test
    public void freestyleJacocoWithOneFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_ANALYSIS_MODEL_FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);

//        assertLineCoverageResults(Arrays.asList(TOTAL_LINES_JACOCO_ANALYSIS_MODEL),
//                Arrays.asList(COVERED_LINES_JACOCO_ANALYSIS_MODEL), coverageResult);
    }

    /** Test with two Jacoco files */
    @Test
    public void freestyleJacocoWithTwoFiles() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_ANALYSIS_MODEL_FILE_NAME, JACOCO_CODING_STYLE_FILE_NAME);

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);

        assertLineCoverageResults(Arrays.asList(JACOCO_ANALYSIS_MODEL_LINES_TOTAL, JACOCO_CODING_STYLE_LINES_TOTAL),
                Arrays.asList(JACOCO_ANALYSIS_MODEL_LINES_COVERED, JACOCO_CODING_STYLE_LINES_COVERED), coverageResult);
    }

    /** Test with two Jacoco files and two adapters */
    @Test
    public void freestyleJacocoWithTwoFilesAndTwoAdapters() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_ANALYSIS_MODEL_FILE_NAME, JACOCO_CODING_STYLE_FILE_NAME);

        JacocoReportAdapter jacocoReportAdapterOne = new JacocoReportAdapter(JACOCO_ANALYSIS_MODEL_FILE_NAME);
        JacocoReportAdapter jacocoReportAdapterTwo = new JacocoReportAdapter(JACOCO_CODING_STYLE_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setAdapters(Arrays.asList(jacocoReportAdapterOne, jacocoReportAdapterTwo));

        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);

        assertLineCoverageResults(Arrays.asList(JACOCO_ANALYSIS_MODEL_LINES_TOTAL, JACOCO_CODING_STYLE_LINES_TOTAL),
                Arrays.asList(JACOCO_ANALYSIS_MODEL_LINES_COVERED, JACOCO_CODING_STYLE_LINES_COVERED), coverageResult);
    }

    /** Test with Cobertura Adapter and no files */
    @Test
    public void freestyleCoberturaWithEmptyFiles() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");
        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult).isNull();
    }

    @Test
    public void freestyleCoberturaWithOneFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);

        assertLineCoverageResults(
                Arrays.asList(COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_TOTAL),
                Arrays.asList(COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_COVERED),
                coverageResult);
    }

    @Test
    public void freestyleCoberturaWithTwoFiles() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME, COBERTURA_COVERAGE_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);

        assertLineCoverageResults(
                Arrays.asList(COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_TOTAL, COBERTURA_COVERAGE_LINES_TOTAL),
                Arrays.asList(COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_COVERED, COBERTURA_COVERAGE_LINES_COVERED),
                coverageResult);
    }

    @Test
    public void freestyleWithJacocoAdapterAndCoberturaFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(
                COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        coveragePublisher.setAdapters(Arrays.asList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);

        assertLineCoverageResults(
                Collections.emptyList(),
                Collections.emptyList(),
                coverageResult);
    }

    @Test
    public void freestyleWithCoberturaAndJacocoFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_CODING_STYLE_FILE_NAME, COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_CODING_STYLE_FILE_NAME);
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(
                COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        coveragePublisher.setAdapters(Arrays.asList(coberturaReportAdapter, jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertLineCoverageResults(
                Arrays.asList(COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_TOTAL, JACOCO_CODING_STYLE_LINES_TOTAL),
                Arrays.asList(COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_COVERED, JACOCO_CODING_STYLE_LINES_COVERED),
                coverageResult);
    }

    @Test
    public void freestyleZeroReportsFail() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        coveragePublisher.setFailNoReports(true);
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.FAILURE);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
    }

    @Test
    public void freestyleZeroReportsOkay() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        coveragePublisher.setFailNoReports(false);
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.SUCCESS);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
    }

    @Test
    public void pipelineZeroReportsFail() {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('*.xml')], failNoReports: true, sourceFileResolver: sourceFiles('NEVER_STORE')"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.FAILURE);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
    }

    @Test
    public void pipelineZeroReportsOkay() {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('*.xml')], failNoReports: false, sourceFileResolver: sourceFiles('NEVER_STORE')"
                + "}", true));

        Run<?, ?> build = buildWithResult(job, Result.SUCCESS);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
    }

    @Test
    public void freestyleQualityGatesSuccessful() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(95f);
        lineThreshold.setFailUnhealthy(true);

        coveragePublisher.setGlobalThresholds(Collections.singletonList(lineThreshold));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.SUCCESS);
    }

    @Test
    public void freestyleQualityGatesSuccessfulUnhealthy() {
//        FreeStyleProject project = createFreeStyleProject();
//        copyFilesToWorkspace(project, JACOCO_ANALYSIS_MODEL_FILE_NAME);
//
//        CoveragePublisher coveragePublisher = new CoveragePublisher();
//        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
//        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
//
//        Threshold lineThreshold = new Threshold("Line");
//        lineThreshold.setUnhealthyThreshold(99f);
//
//        coveragePublisher.setGlobalThresholds(Collections.singletonList(lineThreshold));
//        project.getPublishersList().add(coveragePublisher);
//
//        Run<?, ?> build = buildSuccessfully(project);
//        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
//        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
        // TODO. How to get check health report ?
        //        assertThat(coverageResult.getHealthReport()).isEqualTo(1);
    }

    @Test
    public void freestyleQualityGatesUnstable() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnstableThreshold(99f);

        coveragePublisher.setGlobalThresholds(Collections.singletonList(lineThreshold));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.UNSTABLE);
    }

    @Test
    public void freestyleQualityGatesFail() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(99f);
        lineThreshold.setFailUnhealthy(true);

        coveragePublisher.setGlobalThresholds(Collections.singletonList(lineThreshold));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildWithResult(project, Result.FAILURE);
    }

    @Test
    public void freestyleHealthReports() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_COVERAGE_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        HealthReport healthReport = build.getAction(CoverageBuildAction.class).getHealthReport();

        // TODO: Niko: Health reports are not set?
    }

    @Test
    public void freestyleFailWhenCoverageDecreases() {
        FreeStyleProject project = createFreeStyleProject();
        // build 1
        copyFilesToWorkspace(project, JACOCO_CODING_STYLE_FILE_NAME);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_CODING_STYLE_FILE_NAME);
        coveragePublisher.setAdapters(Arrays.asList(jacocoReportAdapter));
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        // build 2
        copyFilesToWorkspace(project, JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        CoveragePublisher coveragePublisherTwo = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapterTwo = new JacocoReportAdapter(JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        coveragePublisherTwo.setAdapters(Arrays.asList(jacocoReportAdapterTwo));
        coveragePublisherTwo.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        project.getPublishersList().add(coveragePublisherTwo);
        Run<?, ?> build_two = buildWithResult(project, Result.FAILURE);
    }

    @Test
    public void freestyleSkipChecksWhenPublishing() throws IOException {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_COVERAGE_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setSkipPublishingChecks(true);
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);

        List<String> log = build.getLog(20);
        log.forEach(System.out::println);

        // TODO: Niko: How to inject/check the TaskListener for entries?
        // skipPublishingChecks is a flag that either publishes the coverageAction to a TaskListener or not
        // Currently only logging default message "No suitable checks publisher found." (see ChecksPublisher.class)
        // Check for this default message? (possible with build.getLog?
        // Or inject another "real" listener to get the ChecksDetails? How?
    }

    @Test
    public void freestyleSourceCodeRendering() {
        // TODO: How to test ?

//        FreeStyleProject project = createFreeStyleProject();
//
//        // build 1
//        copyFilesToWorkspace(project, JACOCO_CODING_STYLE_FILE_NAME, JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
//        CoveragePublisher coveragePublisher = new CoveragePublisher();
//        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_CODING_STYLE_FILE_NAME);
//        coveragePublisher.setAdapters(Arrays.asList(jacocoReportAdapter));
//        DefaultSourceFileResolver sourceFileResolverNeverStore = new DefaultSourceFileResolver(
//                SourceFileResolverLevel.NEVER_STORE);
//        coveragePublisher.setSourceFileResolver(sourceFileResolverNeverStore);
//        project.getPublishersList().add(coveragePublisher);
//        Run<?, ?> build = buildSuccessfully(project);
//
//        // build 2
//        CoveragePublisher coveragePublisherTwo = new CoveragePublisher();
//        JacocoReportAdapter jacocoReportAdapterTwo = new JacocoReportAdapter(JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
//        coveragePublisherTwo.setAdapters(Arrays.asList(jacocoReportAdapterTwo));
//        coveragePublisherTwo.setFailBuildIfCoverageDecreasedInChangeRequest(true);
//        project.getPublishersList().add(coveragePublisherTwo);
//        Run<?, ?> buildTwo = buildWithResult(project, Result.FAILURE);
    }

    @Test
    public void freestyleSourceCodeCopying() {
        // TODO: How to test ? Difference to rendering ?
    }

    @Test
    public void freestyleDeltaComputation() {
        FreeStyleProject project = createFreeStyleProject();

        // build 1
        copyFilesToWorkspace(project, JACOCO_CODING_STYLE_FILE_NAME, JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_CODING_STYLE_FILE_NAME);
        coveragePublisher.setAdapters(Arrays.asList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        // build 2
        CoveragePublisher coveragePublisherTwo = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapterTwo = new JacocoReportAdapter(JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        coveragePublisherTwo.setAdapters(Arrays.asList(jacocoReportAdapterTwo));
        project.getPublishersList().add(coveragePublisherTwo);
        Run<?, ?> buildTwo = buildSuccessfully(project);

        CoverageBuildAction coverageResult = buildTwo.getAction(CoverageBuildAction.class);

        assertThat(coverageResult.getDelta(CoverageMetric.LINE)).isEqualTo("-0.019");
    }

    @Test
    public void freestyleReferenceBuild() {
        FreeStyleProject project = createFreeStyleProject();

        // build 1
        copyFilesToWorkspace(project, JACOCO_CODING_STYLE_FILE_NAME, JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_CODING_STYLE_FILE_NAME);
        coveragePublisher.setAdapters(Arrays.asList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        // build 2
        CoveragePublisher coveragePublisherTwo = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapterTwo = new JacocoReportAdapter(JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        coveragePublisherTwo.setAdapters(Arrays.asList(jacocoReportAdapterTwo));
        project.getPublishersList().add(coveragePublisherTwo);
        Run<?, ?> buildTwo = buildSuccessfully(project);

        CoverageBuildAction coverageResult = buildTwo.getAction(CoverageBuildAction.class);

        assertThat(coverageResult.getReferenceBuild()).isEqualTo("-0.019");
    }

    @Test
    public void pipelineJacocoWithNoFile() {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult).isNull();
    }

    @Test
    public void pipelineJacocoWithOneFile() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        verifySimpleCoverageNode(job,
                JACOCO_ANALYSIS_MODEL_LINES_COVERED,
                JACOCO_ANALYSIS_MODEL_LINES_TOTAL - JACOCO_ANALYSIS_MODEL_LINES_COVERED);
    }

    @Test
    public void pipelineJacocoWithTwoFiles() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE_NAME,
                JACOCO_CODING_STYLE_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);

        // 6. Mit Assertions Ergebnisse überprüfen
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertLineCoverageResults(Arrays.asList(JACOCO_ANALYSIS_MODEL_LINES_TOTAL, JACOCO_CODING_STYLE_LINES_TOTAL),
                Arrays.asList(JACOCO_ANALYSIS_MODEL_LINES_COVERED, JACOCO_CODING_STYLE_LINES_COVERED), coverageResult);
    }

    @Test
    public void pipelineCoberturaWithNoFile() {
        WorkflowJob job = createPipeline();
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [cobertura('*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult).isNull();
    }

    @Test
    public void pipelineCoberturaWithOneFile() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_COVERAGE_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [cobertura('*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                + "}", true));

        verifySimpleCoverageNode(job,
                COBERTURA_COVERAGE_LINES_COVERED, COBERTURA_COVERAGE_LINES_TOTAL - COBERTURA_COVERAGE_LINES_COVERED);
    }

    @Test
    public void pipelineCoberturaWithTwoFiles() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(COBERTURA_COVERAGE_FILE_NAME,
                COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [cobertura('*.xml')], sourceFileResolver: sourceFiles('NEVER_STORE')"
                + "}", true));

        Run<?, ?> build = buildSuccessfully(job);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertLineCoverageResults(
                Arrays.asList(COBERTURA_COVERAGE_LINES_COVERED, COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_TOTAL),
                Arrays.asList(COBERTURA_COVERAGE_LINES_COVERED, COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_LINES_COVERED),
                coverageResult);
    }

    @Test
    public void pipelineCoberturaAndJacocoFile() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE_NAME,
                COBERTURA_COVERAGE_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml'), cobertura('**/*.xml')]"
                + "}", true));
        Run<?, ?> build = buildSuccessfully(job);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertLineCoverageResults(Arrays.asList(JACOCO_ANALYSIS_MODEL_LINES_TOTAL, COBERTURA_COVERAGE_LINES_TOTAL),
                Arrays.asList(JACOCO_ANALYSIS_MODEL_LINES_COVERED, COBERTURA_COVERAGE_LINES_COVERED), coverageResult);
    }

    @Test
    public void pipelineQualityGatesSuccess() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml', thresholds: [[thresholdTarget: 'Line', unhealthyThreshold: 95.0]])], sourceFileResolver: sourceFiles('NEVER_STORE')"
                + "}", true));
        Run<?, ?> build = buildSuccessfully(job);
    }

    @Test
    public void pipelineQualityGatesSuccessUnhealthy() {
//        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE_NAME);
//        job.setDefinition(new CpsFlowDefinition("node {"
//                + "   publishCoverage adapters: [jacocoAdapter(path: '**/*.xml', thresholds: [[thresholdTarget: 'Line', unhealthyThreshold: 95.0]])], sourceFileResolver: sourceFiles('NEVER_STORE')"
//                + "}", true));
//        Run<?, ?> build = buildSuccessfully(job);
//        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
//        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        // TODO: How to check unhealthy status ? Create Freestyle test as well for this case
    }

    @Test
    public void pipelineQualityGatesUnstable() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter(path: '*.xml', thresholds: [[thresholdTarget: 'Line', unstableThreshold: 99.0]])], sourceFileResolver: sourceFiles('NEVER_STORE')"
                + "}", true));
        Run<?, ?> build = buildWithResult(job, Result.UNSTABLE);
        assertThat(build.getResult()).isEqualTo(Result.UNSTABLE);
    }

    @Test
    public void pipelineFailWhenCoverageDecreases() {
        WorkflowJob jobOne = createPipelineWithWorkspaceFiles(JACOCO_CODING_STYLE_FILE_NAME);
        jobOne.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')], failBuildIfCoverageDecreasedInChangeRequest: true"
                + "}", true));
        Run<?, ?> build = buildWithResult(jobOne, Result.SUCCESS);

        WorkflowJob jobTwo = createPipelineWithWorkspaceFiles(JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        jobTwo.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')], failBuildIfCoverageDecreasedInChangeRequest: true"
                + "}", true));
        Run<?, ?> build2 = buildWithResult(jobTwo, Result.FAILURE);
        assertThat(build2.getResult()).isEqualTo(Result.FAILURE);
    }

    @Test
    public void pipelineQualityGatesFail() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter(path: '*.xml', thresholds: [[failUnhealthy: true, thresholdTarget: 'Line', unhealthyThreshold: 99.0]])], sourceFileResolver: sourceFiles('NEVER_STORE')"
                + "}", true));
        Run<?, ?> build = buildWithResult(job, Result.FAILURE);
    }

    /**
     * Assert line aggregated line coverage of a coverage result
     */
    private void assertLineCoverageResults(List<Integer> totalLines, List<Integer> coveredLines,
            CoverageBuildAction coverageResult) {
        int totalCoveredLines = coveredLines.stream().mapToInt(x -> x).sum();
        int totalMissedLines =
                totalLines.stream().mapToInt(x -> x).sum() - coveredLines.stream().mapToInt(x -> x).sum();
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(
                        totalCoveredLines,
                        totalMissedLines
                ));
    }

    private void verifySimpleCoverageNode(final ParameterizedJob<?, ?> project, int assertCoveredLines,
            int assertMissedLines) {
        // 4. Jacoco XML File in den Workspace legen (Stub für einen Build)
        // 5. Jenkins Build starten
        Run<?, ?> build = buildSuccessfully(project);

        // 6. Mit Assertions Ergebnisse überprüfen
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(assertCoveredLines, assertMissedLines));
    }
}