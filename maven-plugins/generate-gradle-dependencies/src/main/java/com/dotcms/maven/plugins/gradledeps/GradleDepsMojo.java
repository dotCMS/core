package com.dotcms.maven.plugins.gradledeps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.graph.Dependency;


/**
 * Maven plugin that processes the resolved dependencies of the project and
 * generates a file containing gradle dependencies for each dependency.
 */

@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true)
public class GradleDepsMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;


    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Component
    private ProjectDependenciesResolver projectDependenciesResolver;

    // The output location for the Gradle dependencies file
    @Parameter(defaultValue = "${project.build.directory}", property = "outputDirectory", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "dependencies3.gradle", property = "outputFileName", required = true)
    private String outputFileName;


    @Parameter
    private List<Exclusion> exclusions;


    @Override
    public void execute() throws MojoExecutionException {
        try {

            // Make sure the output directory exists
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            // The output file
            File outputFile = new File(outputDirectory, outputFileName);

            DefaultProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
            buildingRequest.setRepositorySession( session.getRepositorySession() );
            buildingRequest.setResolveDependencies( true );

            DependencyResolutionRequest resolutionRequest = new DefaultDependencyResolutionRequest(project, session.getRepositorySession());
            DependencyResolutionResult resolutionResult = projectDependenciesResolver.resolve(resolutionRequest);

            StringBuilder sb = new StringBuilder();
            int count = 0;
            sb.append("dependencies {").append(System.lineSeparator());

            ArrayList<Dependency> sortedList = new ArrayList<>(
                    resolutionResult.getResolvedDependencies());

            sortedList.sort(Comparator.comparing(Dependency::toString));



            for (Dependency artifact : sortedList) {
                if (isExcluded(artifact)) {
                    getLog().info("Skipping excluded artifact: " + artifact);
                    continue;
                }
                count++;
                getLog().debug("Resolved Dependency: " + artifact.toString());
                if (artifact.getScope().equals("system")) {
                    getLog().debug("Skipping system dependency: " + artifact);
                    continue;
                }
                sb.append(convertToGradleDependency(artifact)).append(System.lineSeparator());
            }

            sb.append("}").append(System.lineSeparator());


            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write(sb.toString());
            }

            getLog().info("Writing "+count+" Gradle dependencies to: " + outputFile.getAbsolutePath());

        } catch (IOException | DependencyResolutionException e) {
            throw new MojoExecutionException("Failed to process the dependencies for gradle file", e);
        }
    }

    private <T> Comparable compareKey(Dependency obj1) {
        return obj1.getArtifact().getGroupId() + ":" + obj1.getArtifact().getArtifactId() + ":" + obj1.getArtifact().getVersion();
    }

    private String convertToGradleDependency(Dependency dependency) {
        StringBuilder sb = new StringBuilder();
        sb.append("    ");
        switch(dependency.getScope()) {
            case "compile":
                sb.append("implementation ");
                break;
            case "provided":
                sb.append("compileOnly ");
                break;
            case "runtime":
                sb.append("implementation ");
                break;
            case "test":
                sb.append("testImplementation ");
                break;
            case "system":
                sb.append("system ");
                break;
            default:
                sb.append("implementation ");
                break;
        }


        sb.append("(group: '").append(dependency.getArtifact().getGroupId()).append("', name: '")
                .append(dependency.getArtifact().getArtifactId()).append("', version: '").append(dependency.getArtifact().getVersion()).append("')");

        if (!dependency.getExclusions().isEmpty())
        {
            sb.append(" {\n");
            for (org.eclipse.aether.graph.Exclusion exclusion : dependency.getExclusions())
            {

                if (exclusion.getGroupId().equals("*") && exclusion.getArtifactId().equals("*")) {
                    sb.append("        transitive=false").append("\n");
                } else
                    sb.append("        exclude group: '").append(exclusion.getGroupId()).append("', module: '").append(exclusion.getArtifactId()).append("'\n");
            }
            sb.append("    }");
        } else {
            sb.append("{}");
        }
        return sb.toString();
    }

    private boolean isExcluded(Dependency artifact) {
        for (Exclusion exclusion : exclusions) {
            String artifactId = artifact.getArtifact().getArtifactId();
            String groupId = artifact.getArtifact().getGroupId();
            if ((exclusion.getGroupId().equals("*") || exclusion.getGroupId().equals(groupId)) &&
                    (exclusion.getArtifactId().equals("*") || exclusion.getArtifactId().equals(artifactId))) {
                return true;
            }
        }
        return false;
    }

    public static class Exclusion {
        private String groupId;
        private String artifactId;

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }
    }
}
