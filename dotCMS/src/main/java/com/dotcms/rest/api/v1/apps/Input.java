package com.dotcms.rest.api.v1.apps;

import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.util.Arrays;

/**
 * This class represent an input value captured using a char array The value on the char array can
 * be wiped out as opposed to a string which are final
 */
@JsonDeserialize(using = Input.InputDeserialize.class)
public class Input {

    @NotNull
    private final char[] value;

    private final boolean hidden;

    private Input(final char[] value, final boolean hidden) {
        this.value = trim(value);
        this.hidden = hidden;
    }

    @JsonCreator
    public static Input newInputParam(@JsonProperty("value") final char[] value,
            @JsonProperty("hidden") final boolean hidden) {
        return new Input(value, hidden);
    }

    public static Input newInputParam(final char[] value) {
        return new Input(value, false);
    }

    public char[] getValue() {
        return value;
    }

    public boolean isHidden() {
        return hidden;
    }

    void destroySecret() {
        Arrays.fill(value, (char) 0);
    }

    static final class InputDeserialize extends JsonDeserializer<Input> {

        @Override
        public Input deserialize(final JsonParser jsonParser, final DeserializationContext context)
                throws IOException {
            final JsonNode jsonNode = jsonParser.readValueAsTree();
            final JsonNode value = jsonNode.get("value");
            final JsonNode hidden = jsonNode.get("hidden");
            return newInputParam(value.asText().trim().toCharArray(),
                    hidden != null && hidden.asBoolean());
        }
    }

    /**
     * Removes leading and trailing whitespaces from a char array
     * @param value char[] to go over
     * @return
     */
    private char[] trim(final char[] value){
        final int leftPosition = getTrimLeftPosition(value);
        final int rightPosition = getTrimRightPosition(value);
        return Arrays.copyOfRange(value, leftPosition, rightPosition + 1);
    }

    /**
     * Finds the last position (from left to right) where a whitespace is found in a char array
     * before the first non-whitespace character
     * @param value char[] to go over
     * @return
     */
    private int getTrimLeftPosition(final char[] value){
        int position = 0;
        if(value == null){
            return position;
        }

        while(position < value.length && value[position] == ' ') {
            position++;
        }

        return position;
    }

    /**
     * Finds the last position (from right to left) where a whitespace is found in a char array
     * before the first non-whitespace character
     * @param value char[] to go over
     * @return
     */
    private int getTrimRightPosition(final char[] value){
        int position = value.length - 1;
        if(value == null){
            return position;
        }

        while(position >= 0 && value[position] == ' ') {
            position--;
        }

        return position;
    }

}
