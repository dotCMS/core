package com.dotcms.rendering;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.util.PageMode;

/**
 * This interface allows the mapping of a request to a class that extends the RenderModeHandler
 * class. It allows you to intercept a rendering call to a Page in dotCMS and handle it differently
 * than just sending it on to velocity to render.
 * 
 * @author will
 *
 */
public interface RenderModeMapper {


    
    /**
     * The map needs 5 entries, one for each page mode
     * @return
     */
    public Map<PageMode, RenderModeHandler.Function> getModMop();


    public boolean useModes(HttpServletRequest request);



}
