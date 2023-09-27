package com.dotcms.cli.command;

import com.dotcms.cli.common.OutputOptionMixin;

public interface DotCommand {

    String getName();

    OutputOptionMixin getOutput();

}
