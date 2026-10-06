package com.dotcms.inference.model;

import java.io.Serializable;

/**
 * The caller's preference for whether, and which, tools the model should call.
 *
 * @param mode     how freely the model may choose; required
 * @param function the tool to force, required when and only when the mode is {@link Mode#FUNCTION}
 */
public record ToolChoice(Mode mode, String function) implements Serializable {

    /** How freely the model may pick a tool. */
    public enum Mode {
        /** The model decides whether to call a tool. */
        AUTO,
        /** The model must call some tool. */
        REQUIRED,
        /** The model must not call a tool. */
        NONE,
        /** The model must call the named tool. */
        FUNCTION
    }

    public ToolChoice {
        if (mode == null) {
            throw new IllegalArgumentException("ToolChoice mode is required");
        }
        if (mode == Mode.FUNCTION && (function == null || function.isBlank())) {
            throw new IllegalArgumentException("ToolChoice mode FUNCTION requires a function name");
        }
        if (mode != Mode.FUNCTION && function != null) {
            throw new IllegalArgumentException("ToolChoice names a function only in FUNCTION mode");
        }
    }

    /** @return a choice leaving the decision to the model */
    public static ToolChoice auto() {
        return new ToolChoice(Mode.AUTO, null);
    }

    /**
     * @param function the tool the model must call
     * @return a choice forcing one named tool
     */
    public static ToolChoice function(final String function) {
        return new ToolChoice(Mode.FUNCTION, function);
    }
}
