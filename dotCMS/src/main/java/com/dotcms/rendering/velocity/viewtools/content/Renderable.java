package com.dotcms.rendering.velocity.viewtools.content;

/**
 * A renderable is an object that can transform it self to HTML.
 * It can use their own way (usually using a convention velocity templates structure) or use a different path to overrided the
 * default rendering of one or more elements.
 * @author jsanca
 */
public interface Renderable {

    /**
     * Renders it to html
     * @return String
     */
    String toHtml();

    /**
     * Renders to html using the base template path to override the default markup templates
     * @param baseTemplatePath String folder path, could be have or not the host (starting by //)
     * @return String
     */
    String toHtml(String baseTemplatePath);
}
