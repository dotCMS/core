import { DotCMSContentlet } from '@dotcms/dotcms-models';

import {
    DisabledEditorType,
    getCurrentEditorFromDisabled,
    parseDisabledEditorEntry,
    updateDisabledWYSIWYGOnEditorSwitch
} from './field-editor-preferences.util';

import { AvailableEditorTextArea } from '../../dot-edit-content-text-area/dot-edit-content-text-area.constants';
import { AvailableEditor } from '../../dot-edit-content-wysiwyg-field/dot-edit-content-wysiwyg-field.constant';

describe('field-editor-preferences.util', () => {
    describe('parseDisabledEditorEntry', () => {
        it('should parse textarea Monaco entry correctly', () => {
            const result = parseDisabledEditorEntry('myField@ToggleEditor');
            expect(result).toEqual({
                fieldVariable: 'myField',
                editorType: DisabledEditorType.MonacoTextArea,
                fullEntry: 'myField@ToggleEditor'
            });
        });

        it('should parse WYSIWYG Monaco entry correctly', () => {
            const result = parseDisabledEditorEntry('myField');
            expect(result).toEqual({
                fieldVariable: 'myField',
                editorType: DisabledEditorType.MonacoWYSIWYG,
                fullEntry: 'myField'
            });
        });

        it('should parse legacy PLAIN entry correctly', () => {
            const result = parseDisabledEditorEntry('myField@PLAIN');
            expect(result).toEqual({
                fieldVariable: 'myField',
                editorType: DisabledEditorType.PlainLegacy,
                fullEntry: 'myField@PLAIN'
            });
        });

        it('should handle unknown suffixes', () => {
            const result = parseDisabledEditorEntry('myField@Unknown');
            expect(result).toEqual({
                fieldVariable: 'myField',
                editorType: null,
                fullEntry: 'myField@Unknown'
            });
        });

        it('should handle empty or invalid entries', () => {
            expect(parseDisabledEditorEntry('')).toEqual({
                fieldVariable: '',
                editorType: null,
                fullEntry: ''
            });

            expect(parseDisabledEditorEntry(null)).toEqual({
                fieldVariable: '',
                editorType: null,
                fullEntry: null
            });
        });
    });

    describe('getCurrentEditorFromDisabled', () => {
        describe('for textarea fields', () => {
            it('should return Monaco when ToggleEditor entry exists', () => {
                const disabledWYSIWYG = ['myField@ToggleEditor'];
                const result = getCurrentEditorFromDisabled('myField', disabledWYSIWYG, true);
                expect(result).toBe(AvailableEditorTextArea.Monaco);
            });

            it('should return PlainText when no entry exists', () => {
                const disabledWYSIWYG = ['otherField@ToggleEditor'];
                const result = getCurrentEditorFromDisabled('myField', disabledWYSIWYG, true);
                expect(result).toBe(AvailableEditorTextArea.PlainText);
            });

            it('should return PlainText for empty disabledWYSIWYG', () => {
                const result = getCurrentEditorFromDisabled('myField', [], true);
                expect(result).toBe(AvailableEditorTextArea.PlainText);
            });
        });

        describe('for WYSIWYG fields', () => {
            it('should return Monaco when field variable entry exists', () => {
                const disabledWYSIWYG = ['myField'];
                const result = getCurrentEditorFromDisabled('myField', disabledWYSIWYG, false);
                expect(result).toBe(AvailableEditor.Monaco);
            });

            it('should return Monaco when legacy PLAIN entry exists', () => {
                const disabledWYSIWYG = ['myField@PLAIN'];
                const result = getCurrentEditorFromDisabled('myField', disabledWYSIWYG, false);
                expect(result).toBe(AvailableEditor.Monaco);
            });

            it('should return TinyMCE when no entry exists', () => {
                const disabledWYSIWYG = ['otherField'];
                const result = getCurrentEditorFromDisabled('myField', disabledWYSIWYG, false);
                expect(result).toBe(AvailableEditor.TinyMCE);
            });

            it('should return TinyMCE for empty disabledWYSIWYG', () => {
                const result = getCurrentEditorFromDisabled('myField', [], false);
                expect(result).toBe(AvailableEditor.TinyMCE);
            });
        });
    });

    describe('updateDisabledWYSIWYGOnEditorSwitch', () => {
        describe('for textarea fields', () => {
            it('should add Monaco entry when switching to Monaco', () => {
                const current = ['otherField@ToggleEditor'];
                const result = updateDisabledWYSIWYGOnEditorSwitch(
                    'myField',
                    AvailableEditorTextArea.Monaco,
                    current,
                    true
                );
                expect(result).toEqual(['otherField@ToggleEditor', 'myField@ToggleEditor']);
            });

            it('should remove entry when switching to PlainText', () => {
                const current = ['myField@ToggleEditor', 'otherField@ToggleEditor'];
                const result = updateDisabledWYSIWYGOnEditorSwitch(
                    'myField',
                    AvailableEditorTextArea.PlainText,
                    current,
                    true
                );
                expect(result).toEqual(['otherField@ToggleEditor']);
            });

            it('should replace existing entry when switching to Monaco', () => {
                const current = ['myField@SomeOtherEntry', 'otherField@ToggleEditor'];
                const result = updateDisabledWYSIWYGOnEditorSwitch(
                    'myField',
                    AvailableEditorTextArea.Monaco,
                    current,
                    true
                );
                expect(result).toEqual(['otherField@ToggleEditor', 'myField@ToggleEditor']);
            });
        });

        describe('for WYSIWYG fields', () => {
            it('should add field variable entry when switching to Monaco', () => {
                const current = ['otherField'];
                const result = updateDisabledWYSIWYGOnEditorSwitch(
                    'myField',
                    AvailableEditor.Monaco,
                    current,
                    false
                );
                expect(result).toEqual(['otherField', 'myField']);
            });

            it('should remove entry when switching to TinyMCE', () => {
                const current = ['myField', 'otherField'];
                const result = updateDisabledWYSIWYGOnEditorSwitch(
                    'myField',
                    AvailableEditor.TinyMCE,
                    current,
                    false
                );
                expect(result).toEqual(['otherField']);
            });

            it('should replace legacy PLAIN entry when switching to Monaco', () => {
                const current = ['myField@PLAIN', 'otherField'];
                const result = updateDisabledWYSIWYGOnEditorSwitch(
                    'myField',
                    AvailableEditor.Monaco,
                    current,
                    false
                );
                expect(result).toEqual(['otherField', 'myField']);
            });
        });
    });

    describe('integration scenarios', () => {
        it('should handle complete workflow for textarea field', () => {
            const contentlet: Partial<DotCMSContentlet> = {
                disabledWYSIWYG: []
            };

            // Initially no entry, should default to PlainText
            expect(
                getCurrentEditorFromDisabled('myTextArea', contentlet.disabledWYSIWYG, true)
            ).toBe(AvailableEditorTextArea.PlainText);

            // Switch to Monaco
            contentlet.disabledWYSIWYG = updateDisabledWYSIWYGOnEditorSwitch(
                'myTextArea',
                AvailableEditorTextArea.Monaco,
                contentlet.disabledWYSIWYG,
                true
            );

            expect(contentlet.disabledWYSIWYG).toEqual(['myTextArea@ToggleEditor']);
            expect(
                getCurrentEditorFromDisabled('myTextArea', contentlet.disabledWYSIWYG, true)
            ).toBe(AvailableEditorTextArea.Monaco);

            // Switch back to PlainText
            contentlet.disabledWYSIWYG = updateDisabledWYSIWYGOnEditorSwitch(
                'myTextArea',
                AvailableEditorTextArea.PlainText,
                contentlet.disabledWYSIWYG,
                true
            );

            expect(contentlet.disabledWYSIWYG).toEqual([]);
            expect(
                getCurrentEditorFromDisabled('myTextArea', contentlet.disabledWYSIWYG, true)
            ).toBe(AvailableEditorTextArea.PlainText);
        });

        it('should handle complete workflow for WYSIWYG field with legacy migration', () => {
            const contentlet: Partial<DotCMSContentlet> = {
                disabledWYSIWYG: ['myWysiwyg@PLAIN']
            };

            // Legacy PLAIN entry should map to Monaco
            expect(
                getCurrentEditorFromDisabled('myWysiwyg', contentlet.disabledWYSIWYG, false)
            ).toBe(AvailableEditor.Monaco);

            // Switch to Monaco
            contentlet.disabledWYSIWYG = updateDisabledWYSIWYGOnEditorSwitch(
                'myWysiwyg',
                AvailableEditor.Monaco,
                contentlet.disabledWYSIWYG,
                false
            );

            expect(contentlet.disabledWYSIWYG).toEqual(['myWysiwyg']);
            expect(
                getCurrentEditorFromDisabled('myWysiwyg', contentlet.disabledWYSIWYG, false)
            ).toBe(AvailableEditor.Monaco);

            // Switch back to TinyMCE and migrate legacy entry
            contentlet.disabledWYSIWYG = updateDisabledWYSIWYGOnEditorSwitch(
                'myWysiwyg',
                AvailableEditor.TinyMCE,
                contentlet.disabledWYSIWYG,
                false
            );

            // Legacy entries are handled automatically by getCurrentEditorFromDisabled

            expect(contentlet.disabledWYSIWYG).toEqual([]);
            expect(
                getCurrentEditorFromDisabled('myWysiwyg', contentlet.disabledWYSIWYG, false)
            ).toBe(AvailableEditor.TinyMCE);
        });
    });
});
