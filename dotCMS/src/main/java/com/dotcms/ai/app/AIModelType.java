package com.dotcms.ai.app;

/**
 * Enum representing different types of AI models used in the application.
 * The types include:
 * <ul>
 *   <li>TEXT: Models used for text generation and processing.</li>
 *   <li>IMAGE: Models used for image generation and processing.</li>
 *   <li>EMBEDDINGS: Models used for generating vector embeddings from text or other data.</li>
 *   <li>UNKNOWN: Represents an unknown or unsupported model type.</li>
 * </ul>
 *
 * @author vico
 */
public enum AIModelType {

    TEXT, IMAGE, EMBEDDINGS, UNKNOWN

}
