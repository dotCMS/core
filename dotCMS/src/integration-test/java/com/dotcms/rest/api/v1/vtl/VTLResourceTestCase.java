package com.dotcms.rest.api.v1.vtl;

import com.dotcms.repackage.javax.ws.rs.core.MultivaluedMap;

public class VTLResourceTestCase {

    private final String vtlFile;
    private final String folderName;
    private final MultivaluedMap<String, String> queryParameters;
    private final String pathParameters;
    private final String expectedJSON;
    private final String expectedOutput;
    private final int expectedException;

    public String getVtlFile() {
        return vtlFile;
    }

    public String getFolderName() {
        return folderName;
    }

    public MultivaluedMap<String, String> getQueryParameters() {
        return queryParameters;
    }

    public String getPathParameters() {
        return pathParameters;
    }

    public String getExpectedJSON() {
        return expectedJSON;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public int getExpectedException() {
        return expectedException;
    }

    public VTLResourceTestCase(final String vtlFile, final String folderName, final MultivaluedMap<String, String> queryParameters,
                               final String pathParameters, final String expectedJSON, final String expectedOutput,
                               final int expectedException) {
        this.vtlFile = vtlFile;
        this.folderName = folderName;
        this.queryParameters = queryParameters;
        this.pathParameters = pathParameters;
        this.expectedJSON = expectedJSON;
        this.expectedOutput = expectedOutput;
        this.expectedException = expectedException;
    }

    public static class Builder {
        private String vtlFile;
        private String folderName;
        private MultivaluedMap<String, String> queryParameters;
        private String pathParameters;
        private String expectedJSON;
        private String expectedOutput;
        private int expectedException;

        public Builder setVtlFile(String vtlFile) {
            this.vtlFile = vtlFile;
            return this;
        }

        public Builder setFolderName(String folderName) {
            this.folderName = folderName;
            return this;
        }

        public Builder setQueryParameters(MultivaluedMap<String, String> queryParameters) {
            this.queryParameters = queryParameters;
            return this;
        }

        public Builder setPathParameters(String pathParameters) {
            this.pathParameters = pathParameters;
            return this;
        }

        public Builder setExpectedJSON(String expectedJSON) {
            this.expectedJSON = expectedJSON;
            return this;
        }

        public Builder setExpectedOutput(String expectedOutput) {
            this.expectedOutput = expectedOutput;
            return this;
        }

        public Builder setExpectedException(int expectedException) {
            this.expectedException = expectedException;
            return this;
        }

        public VTLResourceTestCase build() {
            return new VTLResourceTestCase(vtlFile, folderName, queryParameters, pathParameters ,expectedJSON,
                    expectedOutput, expectedException);
        }
    }
}
