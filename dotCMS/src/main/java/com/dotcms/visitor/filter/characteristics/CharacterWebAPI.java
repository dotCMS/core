package com.dotcms.visitor.filter.characteristics;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

/**
 * Character Web API
 * @author jsanca
 */
public interface CharacterWebAPI {

    String DOT_CHARACTER = "dotCharacter";

    /**
     * Get or create a character
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return Character
     */
    Character getOrCreateCharacter(final HttpServletRequest request, final HttpServletResponse response);

    /**
     * Get a character if exist
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return Character
     */
    Optional<Character> getCharacterIfExist(final HttpServletRequest request, final HttpServletResponse response);
}
