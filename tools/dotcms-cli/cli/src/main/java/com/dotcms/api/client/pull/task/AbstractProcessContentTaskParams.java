package com.dotcms.api.client.pull.task;

import com.dotcms.api.client.MapperService;
import com.dotcms.api.client.pull.PullHandler;
import com.dotcms.cli.common.InputOutputFormat;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.annotation.ValueType;
import java.io.File;
import org.immutables.value.Value;
import org.jboss.logging.Logger;

/**
 * Just a class to compile all the params shared by various Pull APIs
 */
@ValueType
@Value.Immutable
public interface AbstractProcessContentTaskParams<T> {

    File destination();

    T content();

    PullHandler<T> pullHandler();

    InputOutputFormat format();

    MapperService mapperService();

    OutputOptionMixin output();

    Logger logger();

}
