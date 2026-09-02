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
    };
}

export const VELOCITY_LANGUAGE_ID = 'velocity-playground';

/**
 * Enriched Velocity grammar tuned for the playground.
 *
 * The grammar in @dotcms/edit-content only emits tokens for `#directive`,
 * `$variable`, and comments — everything else (strings, numbers, method calls,
 * operators) falls through HTML_BASE_TOKENIZER as an empty token and stays
 * uncolored regardless of the active theme. We define a playground-only grammar
 * that adds the missing token classes so Monaco's default theme can
 * distinguish every category developers expect to see.
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

    registered = true;
};
