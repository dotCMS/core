package com.dotcms.rendering.velocity.viewtools;

import org.apache.velocity.tools.view.tools.ViewTool;
import com.dotmarketing.util.Logger;

/**
 * We return an empty string because we 
 * do not want any printout when this tool is used
 */
public class VelocityLoggerTool implements ViewTool {

    final String EMPTY="".intern();

    public String info(final String log) {
        Logger.info(VelocityLoggerTool.class.getName(), log);
        return EMPTY;
    }

    public String warn(final String log) {
        Logger.warn(VelocityLoggerTool.class.getName(), log);
        return EMPTY;
    }

    public String error(final String log) {
        Logger.error(VelocityLoggerTool.class.getName(), log);
        return EMPTY;
    }
    
    public String debug(final String log) {
        Logger.debug(VelocityLoggerTool.class.getName(), log);
        return EMPTY;
    }

    @Override
    public void init(Object arg0) {

    }


    @Override
    public String toString() {
        return EMPTY;
    }

}
