package com.dotcms.cli.common;

import picocli.CommandLine.ExitCode;
import picocli.CommandLine.IExitCodeExceptionMapper;
import picocli.CommandLine.ParameterException;

public class DotExitCodeExceptionMapper implements IExitCodeExceptionMapper {
    @Override
    public int getExitCode(Throwable t) {
        // customize exit code
        // We usually throw an IllegalArgumentException to denote that an invalid param has been passed
        if (t instanceof ParameterException || t instanceof IllegalArgumentException) {
            return ExitCode.USAGE;
        }
        return ExitCode.SOFTWARE;
    }
}
