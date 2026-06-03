interface ThemeRule {
    token: string;
    foreground?: string;
    background?: string;
    fontStyle?: string;
}

interface ThemeData {
    base: 'vs' | 'vs-dark' | 'hc-black' | 'hc-light';
    inherit: boolean;
    rules: ThemeRule[];
    colors: Record<string, string>;
}

interface WindowWithMonaco extends Window {
    monaco?: {
        languages: {
            register: (language: {
                id: string;
                extensions?: string[];
                mimetypes?: string[];
            }) => void;
            setMonarchTokensProvider: (id: string, provider: unknown) => void;
            getLanguages?: () => Array<{ id: string }>;
        };
        editor: {
            defineTheme: (name: string, theme: ThemeData) => void;
            setTheme: (name: string) => void;
        };
    };
}

export const VELOCITY_LANGUAGE_ID = 'velocity-playground';
export const VELOCITY_THEME_ID = 'dot-velocity-dark';

/**
 * Enriched Velocity grammar tuned for the playground.
 *
 * The grammar in @dotcms/edit-content only emits tokens for `#directive`,
 * `$variable`, and comments — everything else (strings, numbers, method calls,
 * operators) falls through HTML_BASE_TOKENIZER as an empty token and stays
 * uncolored regardless of the active theme. We define a playground-only grammar
 * that adds the missing token classes so the Monokai Pro palette below can
 * paint every category developers expect to see distinguished.
 */
const VELOCITY_PLAYGROUND_GRAMMAR = {
    defaultToken: '',
    tokenPostfix: '.vtl',
    ignoreCase: true,

    brackets: [
        { open: '{', close: '}', token: 'delimiter.curly' },
        { open: '[', close: ']', token: 'delimiter.square' },
        { open: '(', close: ')', token: 'delimiter.parenthesis' }
    ],

    keywords: [
        'foreach',
        'if',
        'else',
        'elseif',
        'end',
        'set',
        'parse',
        'include',
        'macro',
        'stop',
        'dotParse'
    ],

    tokenizer: {
        root: [
            // Block + line comments
            [/#\*[\s\S]*?\*#/, 'comment.velocity'],
            [/##.*$/, 'comment.velocity'],

            // Velocity directives — match before generic identifiers so `#set` etc. win
            [/#dotParse\b/, 'keyword.dotparse.velocity'],
            [/#(foreach|if|else|elseif|end|set|parse|include|macro|stop)\b/, 'keyword.velocity'],

            // Velocity variables — `$name`, `${name}`, optional silent `!`
            [/\$!?\{[^}]+\}/, 'variable.velocity'],
            [/\$!?[a-zA-Z_][a-zA-Z0-9_]*/, 'variable.velocity'],

            // Property / method access — `.name` that follows a variable / call result
            [/\.([a-zA-Z_][a-zA-Z0-9_]*)/, 'identifier.method.velocity'],

            // Strings (double, single, triple-double for VTL multi-line)
            [/"""/, { token: 'string.velocity', next: '@stringTriple' }],
            [/"/, { token: 'string.velocity', next: '@stringDouble' }],
            [/'/, { token: 'string.velocity', next: '@stringSingle' }],

            // Numbers
            [/\b\d+\.\d+\b/, 'number.float.velocity'],
            [/\b\d+\b/, 'number.velocity'],

            // Operators and delimiters
            [/==|!=|<=|>=|&&|\|\||[<>]/, 'operator.velocity'],
            [/[=+\-*/%]/, 'operator.velocity'],
            [/[,:;]/, 'delimiter.velocity'],
            [/[{}()[\]]/, '@brackets']
        ],

        stringDouble: [
            [/[^"\\]+/, 'string.velocity'],
            [/\\./, 'string.escape.velocity'],
            [/"/, { token: 'string.velocity', next: '@pop' }]
        ],

        stringSingle: [
            [/[^'\\]+/, 'string.velocity'],
            [/\\./, 'string.escape.velocity'],
            [/'/, { token: 'string.velocity', next: '@pop' }]
        ],

        stringTriple: [
            [/[^"]+/, 'string.velocity'],
            [/"""/, { token: 'string.velocity', next: '@pop' }],
            [/"/, 'string.velocity']
        ]
    }
};

/**
 * Monokai-Pro–inspired palette matching the reference screenshot:
 * directives pink/coral, variables orange, method access cyan,
 * strings green, numbers purple, on a very dark surface.
 */
const VELOCITY_THEME: ThemeData = {
    base: 'vs-dark',
    inherit: true,
    rules: [
        // Directives — #set, #if, #foreach, #end, #macro, #parse, #include, #stop, #else, #elseif
        { token: 'keyword.velocity', foreground: 'FF6188', fontStyle: 'bold' },
        // #dotParse — dotCMS-specific directive
        { token: 'keyword.dotparse.velocity', foreground: 'FF6188', fontStyle: 'bold' },

        // Variables — $name, ${name}
        { token: 'variable.velocity', foreground: 'FC9867' },

        // Property / method access — .pull, .identifier, .title, …
        { token: 'identifier.method.velocity', foreground: '78DCE8' },

        // Strings
        { token: 'string.velocity', foreground: 'A9DC76' },
        { token: 'string.escape.velocity', foreground: 'AB9DF2' },

        // Numbers
        { token: 'number.velocity', foreground: 'AB9DF2' },
        { token: 'number.float.velocity', foreground: 'AB9DF2' },

        // Comments
        { token: 'comment.velocity', foreground: '727072', fontStyle: 'italic' },

        // Operators and delimiters
        { token: 'operator.velocity', foreground: 'FF6188' },
        { token: 'delimiter.velocity', foreground: 'FCFCFA' },
        { token: 'delimiter.curly', foreground: 'FCFCFA' },
        { token: 'delimiter.square', foreground: 'FCFCFA' },
        { token: 'delimiter.parenthesis', foreground: 'FCFCFA' }
    ],
    colors: {
        'editor.background': '#2D2A2E',
        'editor.foreground': '#FCFCFA',
        'editorLineNumber.foreground': '#5B595C',
        'editorLineNumber.activeForeground': '#C1C0C0',
        'editor.selectionBackground': '#403E41',
        'editor.lineHighlightBackground': '#34313680',
        'editorCursor.foreground': '#FFD866',
        'editorIndentGuide.background': '#403E41',
        'editorIndentGuide.activeBackground': '#5B595C'
    }
};

let registered = false;

export const ensureVelocityLanguageRegistered = (): void => {
    if (registered) return;

    const win = window as WindowWithMonaco;
    const monaco = win.monaco;
    if (!monaco) return;

    const knownLanguages = monaco.languages.getLanguages?.() ?? [];
    const already = knownLanguages.some((lang) => lang.id === VELOCITY_LANGUAGE_ID);
    if (!already) {
        monaco.languages.register({
            id: VELOCITY_LANGUAGE_ID,
            extensions: ['.vtl'],
            mimetypes: ['text/x-velocity']
        });
        monaco.languages.setMonarchTokensProvider(
            VELOCITY_LANGUAGE_ID,
            VELOCITY_PLAYGROUND_GRAMMAR
        );
    }

    monaco.editor.defineTheme(VELOCITY_THEME_ID, VELOCITY_THEME);
    // setTheme is global — any existing editor instance picks up the change
    // immediately, which fixes the race where ngx-monaco-editor created the
    // editor before defineTheme ran and fell back to the default light theme.
    monaco.editor.setTheme(VELOCITY_THEME_ID);

    registered = true;
};
