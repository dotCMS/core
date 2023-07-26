package com.dotcms.cli.command;

import com.dotcms.cli.common.OutputOptionMixin;

public interface Command {

    String getName();

    OutputOptionMixin getOutput();

}
