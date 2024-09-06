package com.dotcms.api.client.exception;

/**
 * {@code MappingException} is a specialized {@code RuntimeException} that is thrown when there is a
 * problem with mapping an object to a specific type.
 *
 * <p>{@code MappingException} provides constructors to create an instance with a custom error
 * message or with a custom error message and a cause.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try {
 *     // Mapping code here
 * } catch (MappingException ex) {
 *     // Handle mapping exception
 * }
 * }</pre>
 *
 * @see RuntimeException
 */
public class MappingException extends RuntimeException {

    public MappingException(String message) {
        super(message);
    }

    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }
}