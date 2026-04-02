import { addEditorPageScript, SDK_EDITOR_SCRIPT_SOURCE } from './ema-legacy-script-injection';

describe('ema-legacy-script-injection', () => {
    describe('SDK_EDITOR_SCRIPT_SOURCE', () => {
        it('should point to the UVE editor script path', () => {
            expect(SDK_EDITOR_SCRIPT_SOURCE).toBe('/ext/uve/dot-uve.js');
        });
    });

    describe('addEditorPageScript', () => {
        const expectedScript = `<script src="${SDK_EDITOR_SCRIPT_SOURCE}"></script>`;

        it('should insert the script tag before </body> when body tag exists', () => {
            const html = '<html><head></head><body><p>Hello</p></body></html>';
            const result = addEditorPageScript(html);

            expect(result).toContain(expectedScript + '</body>');
            expect(result).toBe(
                `<html><head></head><body><p>Hello</p>${expectedScript}</body></html>`
            );
        });

        it('should append the script tag at the end when no body tag exists', () => {
            const html = '<div>Advanced template content</div>';
            const result = addEditorPageScript(html);

            expect(result).toBe(html + expectedScript);
        });

        it('should handle empty string input', () => {
            const result = addEditorPageScript('');
            expect(result).toBe(expectedScript);
        });

        it('should handle undefined input (defaults to empty string)', () => {
            const result = addEditorPageScript();
            expect(result).toBe(expectedScript);
        });
    });
});
