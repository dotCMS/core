package com.dotcms.api.client.pull.task;

import com.dotcms.api.client.MapperService;
import com.dotcms.api.client.pull.PullHandler;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.InputOutputFormat;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.annotation.ValueType;
import java.io.File;
import java.io.Serializable;
import java.util.List;
import org.immutables.value.Value;
import org.jboss.logging.Logger;

/**
 * Just a class to compile all the params shared by various Pull APIs
 */
@ValueType
@Value.Immutable
public interface AbstractPullTaskParams<T> extends Serializable {

    File destination();

    List<T> contents();

    PullHandler<T> pullHandler();

    InputOutputFormat format();

    MapperService mapperService();

    OutputOptionMixin output();

    Logger logger();

    ConsoleProgressBar progressBar();

}
