import { describe, expect, it, jest } from '@jest/globals';

import { detectEditorType } from './content-type-detector.util';

import { hasMonacoMarker } from '../../../shared/dot-edit-content-monaco-editor-control/monaco-marker.util';
import { AvailableEditorTextArea } from '../dot-edit-content-text-area.constants';

// Mock the hasMonacoMarker function
jest.mock('../../../shared/dot-edit-content-monaco-editor-control/monaco-marker.util');

// Get the mocked version of the function
const mockedHasMonacoMarker = jest.mocked(hasMonacoMarker);

describe('Content Type Detector Utils', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('detectEditorType', () => {
        it('should return Monaco editor type when content has Monaco marker', () => {
            // Arrange
            const content = 'Some content with Monaco marker';
            mockedHasMonacoMarker.mockReturnValue(true);

            // Act
            const result = detectEditorType(content);

            // Assert
            expect(result).toBe(AvailableEditorTextArea.Monaco);
            expect(mockedHasMonacoMarker).toHaveBeenCalledWith(content);
        });

        it('should return PlainText editor type when content does not have Monaco marker', () => {
            // Arrange
            const content = 'This is just plain text without Monaco marker.';
            mockedHasMonacoMarker.mockReturnValue(false);

            // Act
            const result = detectEditorType(content);

            // Assert
            expect(result).toBe(AvailableEditorTextArea.PlainText);
            expect(mockedHasMonacoMarker).toHaveBeenCalledWith(content);
        });

        it('should return PlainText editor type for empty string', () => {
            // Arrange
            const content = '';
            mockedHasMonacoMarker.mockReturnValue(false);

            // Act
            const result = detectEditorType(content);

            // Assert
            expect(result).toBe(AvailableEditorTextArea.PlainText);
            expect(mockedHasMonacoMarker).toHaveBeenCalledWith(content);
        });

        it('should call hasMonacoMarker for any content type', () => {
            // Arrange
            const testCases = [
                'function test() { return true; }', // JavaScript-like
                '<div>HTML content</div>', // HTML-like
                '# Markdown header', // Markdown-like
                '#set($var = "value")', // Velocity-like
                'Plain text content' // Plain text
            ];

            testCases.forEach((content, index) => {
                // Mock return value alternating between true and false for variety
                mockedHasMonacoMarker.mockReturnValue(index % 2 === 0);

                // Act
                const result = detectEditorType(content);

                // Assert
                expect(mockedHasMonacoMarker).toHaveBeenCalledWith(content);
                expect(result).toBe(
                    index % 2 === 0
                        ? AvailableEditorTextArea.Monaco
                        : AvailableEditorTextArea.PlainText
                );
            });
        });
    });
});
