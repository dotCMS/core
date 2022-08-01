package com.dotcms.cli.command;

import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import org.jboss.logging.Logger;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@ActivateRequestContext
public class ContentTypeCommand implements Callable<Integer> {

    @Override
    public Integer call() {


        return ExitCode.OK;
    }
}
