package com.dotcms.visitor.filter.characteristics;

import com.dotcms.visitor.filter.logger.VisitorLogger;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;
import java.util.Optional;

/**
 * Character Web API
 * @author jsanca
 */
public class CharacterWebAPIImpl implements CharacterWebAPI {

    @Override
    public Character getOrCreateCharacter(final HttpServletRequest request, final HttpServletResponse response) {

        final Optional<Character> characterOptional = getCharacterIfExist(request, response);

        if (characterOptional.isPresent()) {
            return characterOptional.get();
        }

        final Character character  = Try.of(()->VisitorLogger.createCharacter(request, response)).getOrNull();

        if (Objects.nonNull(character)) {

            request.setAttribute(DOT_CHARACTER, character);
        }

        return character;
    }

    @Override
    public Optional<Character> getCharacterIfExist(final HttpServletRequest request, final HttpServletResponse response) {

        final Character character = (Character) request.getAttribute(DOT_CHARACTER);
        return Optional.ofNullable(character);
    }
}
