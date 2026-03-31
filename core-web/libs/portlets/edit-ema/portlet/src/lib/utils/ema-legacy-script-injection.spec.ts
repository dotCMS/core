import { addEditorPageScript, SDK_EDITOR_SCRIPT_SOURCE } from './ema-legacy-script-injection';

describe('ema-legacy-script-injection', () => {
    describe('SDK_EDITOR_SCRIPT_SOURCE', () => {
        it('should have the correct value', () => {
            expect(SDK_EDITOR_SCRIPT_SOURCE).toBe('/ext/uve/dot-uve.js');
        });
    });

    describe('addEditorPageScript', () => {
        it('should inject script before </body> when body tag exists', () => {
            const rendered = '<html><body><p>Hello</p></body></html>';
            const result = addEditorPageScript(rendered);
            expect(result).toBe(
                `<html><body><p>Hello</p><script src="${SDK_EDITOR_SCRIPT_SOURCE}"></script></body></html>`
            );
        });

        it('should append script when no </body> tag exists', () => {
            const rendered = '<p>Hello</p>';
            const result = addEditorPageScript(rendered);
            expect(result).toBe(`<p>Hello</p><script src="${SDK_EDITOR_SCRIPT_SOURCE}"></script>`);
        });

        it('should handle empty string', () => {
            const result = addEditorPageScript('');
            expect(result).toBe(`<script src="${SDK_EDITOR_SCRIPT_SOURCE}"></script>`);
        });

        it('should handle undefined input', () => {
            const result = addEditorPageScript(undefined);
            expect(result).toBe(`<script src="${SDK_EDITOR_SCRIPT_SOURCE}"></script>`);
        });
    });
});
