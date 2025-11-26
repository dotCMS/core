import { describe, expect, it, jest } from '@jest/globals';

import { detectEditorType } from './content-type-detector.util';

import {
    isHtml,
    isJavascript,
    isMarkdown,
    isVelocity
} from '../../dot-edit-content-wysiwyg-field/dot-edit-content-wysiwyg-field.utils';
import { AvailableEditorTextArea } from '../dot-edit-content-text-area.constants';

// Mock the language detector functions
jest.mock('../../dot-edit-content-wysiwyg-field/dot-edit-content-wysiwyg-field.utils');

// Get the mocked versions of the functions
const mockedIsVelocity = jest.mocked(isVelocity);
const mockedIsJavascript = jest.mocked(isJavascript);
const mockedIsHtml = jest.mocked(isHtml);
const mockedIsMarkdown = jest.mocked(isMarkdown);

describe('Content Type Detector Utils', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('detectEditorType', () => {
        it('should return Monaco editor type when content contains Velocity syntax', () => {
            // Arrange
            const content = '#foreach($item in $items) $item #end';
            mockedIsVelocity.mockReturnValue(true);
            mockedIsJavascript.mockReturnValue(false);
            mockedIsHtml.mockReturnValue(false);
            mockedIsMarkdown.mockReturnValue(false);

            // Act
            const result = detectEditorType(content);

            // Assert
            expect(result).toBe(AvailableEditorTextArea.Monaco);
            expect(mockedIsVelocity).toHaveBeenCalledWith(content);
        });

        it('should return Monaco editor type when content contains JavaScript code', () => {
            // Arrange
            const content = 'function sayHello() { console.log("Hello"); }';
            mockedIsVelocity.mockReturnValue(false);
            mockedIsJavascript.mockReturnValue(true);
            mockedIsHtml.mockReturnValue(false);
            mockedIsMarkdown.mockReturnValue(false);

            // Act
            const result = detectEditorType(content);

            // Assert
            expect(result).toBe(AvailableEditorTextArea.Monaco);
            expect(mockedIsJavascript).toHaveBeenCalledWith(content);
        });

        it('should return Monaco editor type when content contains HTML markup', () => {
            // Arrange
            const content = '<div><p>Hello World</p></div>';
            mockedIsVelocity.mockReturnValue(false);
            mockedIsJavascript.mockReturnValue(false);
            mockedIsHtml.mockReturnValue(true);
            mockedIsMarkdown.mockReturnValue(false);

            // Act
            const result = detectEditorType(content);

            // Assert
            expect(result).toBe(AvailableEditorTextArea.Monaco);
            expect(mockedIsHtml).toHaveBeenCalledWith(content);
        });

        it('should return Monaco editor type when content contains Markdown syntax', () => {
            // Arrange
            const content = '# Heading\n\n- List item 1\n- List item 2';
            mockedIsVelocity.mockReturnValue(false);
            mockedIsJavascript.mockReturnValue(false);
            mockedIsHtml.mockReturnValue(false);
            mockedIsMarkdown.mockReturnValue(true);

            // Act
            const result = detectEditorType(content);

            // Assert
            expect(result).toBe(AvailableEditorTextArea.Monaco);
            expect(mockedIsMarkdown).toHaveBeenCalledWith(content);
        });

        it('should return PlainText editor type when content does not contain code syntax', () => {
            // Arrange
            const content = 'This is just plain text with no special syntax.';
            mockedIsVelocity.mockReturnValue(false);
            mockedIsJavascript.mockReturnValue(false);
            mockedIsHtml.mockReturnValue(false);
            mockedIsMarkdown.mockReturnValue(false);

            // Act
            const result = detectEditorType(content);

            // Assert
            expect(result).toBe(AvailableEditorTextArea.PlainText);
            expect(mockedIsVelocity).toHaveBeenCalledWith(content);
            expect(mockedIsJavascript).toHaveBeenCalledWith(content);
            expect(mockedIsHtml).toHaveBeenCalledWith(content);
            expect(mockedIsMarkdown).toHaveBeenCalledWith(content);
        });

        it('should return Monaco editor type when any detector returns true', () => {
            // Arrange
            const content = 'Mixed content with <div>HTML</div> and ## Markdown';
            mockedIsVelocity.mockReturnValue(false);
            mockedIsJavascript.mockReturnValue(false);
            mockedIsHtml.mockReturnValue(true);
            mockedIsMarkdown.mockReturnValue(true);

            // Act
            const result = detectEditorType(content);

            // Assert
            expect(result).toBe(AvailableEditorTextArea.Monaco);
            // Verify that it short-circuits once it finds a match
            expect(mockedIsVelocity).toHaveBeenCalledWith(content);
            expect(mockedIsJavascript).toHaveBeenCalledWith(content);
            expect(mockedIsHtml).toHaveBeenCalledWith(content);
            // isMarkdown might not be called due to short-circuiting
        });

        it('should call all detectors in the correct order', () => {
            // Arrange
            const content = 'Some content';
            mockedIsVelocity.mockReturnValue(false);
            mockedIsJavascript.mockReturnValue(false);
            mockedIsHtml.mockReturnValue(false);
            mockedIsMarkdown.mockReturnValue(false);

            // Act
            detectEditorType(content);

            // Assert - check the order of calls
            const callOrder = [
                mockedIsVelocity.mock.invocationCallOrder[0],
                mockedIsJavascript.mock.invocationCallOrder[0],
                mockedIsHtml.mock.invocationCallOrder[0],
                mockedIsMarkdown.mock.invocationCallOrder[0]
            ];

            // Verify calls are in ascending order (the natural order they were called)
            expect(callOrder).toEqual([...callOrder].sort((a, b) => a - b));
        });

        it('should test empty string content correctly', () => {
            // Arrange
            const content = '';
            mockedIsVelocity.mockReturnValue(false);
            mockedIsJavascript.mockReturnValue(false);
            mockedIsHtml.mockReturnValue(false);
            mockedIsMarkdown.mockReturnValue(false);

            // Act
            const result = detectEditorType(content);

            // Assert
            expect(result).toBe(AvailableEditorTextArea.PlainText);
        });
    });
});
