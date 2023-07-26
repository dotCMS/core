package com.dotcms.cli.common;

import static io.quarkus.devtools.messagewriter.MessageIcons.ERROR_ICON;
import static io.quarkus.devtools.messagewriter.MessageIcons.WARN_ICON;

import io.quarkus.arc.DefaultBean;
import io.quarkus.devtools.messagewriter.MessageWriter;
import java.io.PrintWriter;
import javax.enterprise.context.ApplicationScoped;
import picocli.CommandLine;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.Model.CommandSpec;

@DefaultBean
@ApplicationScoped
public class DefaultMessageWriterImpl implements MessageWriter {

    protected boolean debug;

    @CommandLine.Spec(CommandLine.Spec.Target.MIXEE)
    CommandSpec mixee;

    ColorScheme scheme;
    PrintWriter out;
    PrintWriter err;

    /**
     * Constructor. Intended for code reuse.
     * @param debug
     * @param scheme
     * @param out
     * @param err
     */
    public DefaultMessageWriterImpl(boolean debug, ColorScheme scheme, PrintWriter out,
            PrintWriter err) {
        this.debug = debug;
        this.scheme = scheme;
        this.out = out;
        this.err = err;
    }

    /**
     * Default constructor. Used by CDI.
     */
    public DefaultMessageWriterImpl() {
    }

    ColorScheme colorScheme() {
        ColorScheme colors = scheme;
        if (colors == null) {
            colors = scheme = mixee.commandLine().getColorScheme();
        }
        return colors;
    }

     PrintWriter out() {
        PrintWriter o = out;
        if (o == null) {
            o = out = mixee.commandLine().getOut();
        }
        return o;
    }

     PrintWriter err() {
        PrintWriter e = err;
        if (e == null) {
            e = err = mixee.commandLine().getErr();
        }
        return e;
    }

    @Override
    public void info(final String msg) {
        out().println(colorScheme().ansi().new Text(msg, colorScheme()));
    }

    @Override
    public void error(final String msg) {
        err().println(colorScheme().errorText(ERROR_ICON + " " + msg));
    }

    @Override
    public void debug(final String msg) {
        if (isDebugEnabled()) {
            out().println(colorScheme().ansi().new Text("@|faint [DEBUG] " + msg + "|@", colorScheme()));
        }
    }

    @Override
    public void warn(final String msg) {
        out().println(colorScheme().ansi().new Text("@|yellow " + WARN_ICON + " " + msg + "|@", colorScheme()));
    }

    @Override
    public boolean isDebugEnabled() {
        return debug;
    }


}
