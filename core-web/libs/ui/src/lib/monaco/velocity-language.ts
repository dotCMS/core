/**
 * The Velocity (VTL) language for Monaco: the one grammar every dotCMS editor highlights Velocity
 * with — the Velocity Playground, the content editor's code fields and file editor, and the Edit
 * Source panel.
 *
 * Monaco is loaded at runtime through ngx-monaco-editor's AMD loader, so the language is registered
 * against the global `monaco` once it exists instead of importing `monaco-editor`, which would pull
 * the whole bundle into the lib.
 */
import { VELOCITY_HTML_ROOT, VELOCITY_HTML_STATES } from './velocity-html-tokenizer';

/** The subset of Monaco's global namespace needed to register a language. */
interface MonacoLanguagesNamespace {
    languages: {
        register: (language: { id: string; extensions?: string[]; mimetypes?: string[] }) => void;
        setMonarchTokensProvider: (id: string, provider: unknown) => void;
        getLanguages: () => Array<{ id: string }>;
    };
}

/**
 * Monaco language id for Velocity. `velocity` is the id the content editor's code fields and file
 * editor have always used, so their stored and detected languages keep resolving to this grammar.
 */
export const DOT_VELOCITY_LANGUAGE_ID = 'velocity';

/** Velocity that may appear anywhere: in text, inside a tag, or inside an attribute value. */
const VELOCITY_ANYWHERE = [
    [/#\*/, 'comment.velocity', '@velocityBlockComment'],
    [/##.*$/, 'comment.velocity'],

    // Directives that take arguments. The parenthesis opens an expression, which is the only place
    // strings, numbers and operators are coloured: in plain markup an apostrophe is just text.
    [
        /(#\{?(?:foreach|if|elseif|set|parse|include|macro|evaluate|define)\}?)(\s*)(\()/,
        ['keyword.velocity', '', { token: 'delimiter.parenthesis', next: '@velocityArguments' }]
    ],
    [
        /(#\{?dotParse\}?)(\s*)(\()/,
        [
            'keyword.dotparse.velocity',
            '',
            { token: 'delimiter.parenthesis', next: '@velocityArguments' }
        ]
    ],
    [/#\{?(?:else|end|stop|break)\b\}?/, 'keyword.velocity'],

    // References: `$name`, `$!name`, `${name}`, `$!{name}`, followed by any `.property`, `.method()`
    // or `[index]` chain.
    [/\$!?\{/, 'variable.velocity', '@velocityBracedReference'],
    [/\$!?[a-zA-Z_][\w-]*/, 'variable.velocity', '@velocityReference']
];

/** What can follow a reference: properties, method calls and indexes, in any order. */
const VELOCITY_REFERENCE_CHAIN = [
    [
        /(\.)([a-zA-Z_]\w*)(\()/,
        [
            'delimiter',
            'identifier.method.velocity',
            { token: 'delimiter.parenthesis', next: '@velocityArguments' }
        ]
    ],
    [/(\.)([a-zA-Z_]\w*)/, ['delimiter', 'identifier.method.velocity']],
    [/\[/, 'delimiter.square', '@velocityIndex']
];

/** The inside of a Velocity expression: directive arguments, method arguments, list and map literals. */
const VELOCITY_EXPRESSION = [
    [/\$!?\{/, 'variable.velocity', '@velocityBracedReference'],
    [/\$!?[a-zA-Z_][\w-]*/, 'variable.velocity', '@velocityReference'],
    [/"/, 'string.velocity', '@velocityStringDouble'],
    [/'/, 'string.velocity', '@velocityStringSingle'],
    [/\b(?:true|false|null|in|and|or|not|eq|ne|lt|gt|le|ge)\b/, 'keyword.velocity'],
    [/\d+\.\d+/, 'number.float.velocity'],
    [/\d+/, 'number.velocity'],
    [/\.\./, 'operator.velocity'],
    [/==|!=|<=|>=|&&|\|\||[<>!]/, 'operator.velocity'],
    [/[=+\-*/%]/, 'operator.velocity'],
    [/[,:;]/, 'delimiter.velocity'],
    [/\[/, 'delimiter.square', '@velocityIndex'],
    [/\{/, 'delimiter.curly', '@velocityMap'],
    [/[a-zA-Z_]\w*/, 'identifier.velocity'],
    [/\s+/, ''],
    [/./, '']
];

/** Monarch grammar for Velocity templates: HTML with Velocity anywhere in it. */
const DOT_VELOCITY_GRAMMAR = {
    defaultToken: '',
    tokenPostfix: '.vtl',
    ignoreCase: true,

    brackets: [
        { open: '{', close: '}', token: 'delimiter.curly' },
        { open: '[', close: ']', token: 'delimiter.square' },
        { open: '(', close: ')', token: 'delimiter.parenthesis' },
        { open: '<', close: '>', token: 'delimiter.angle' }
    ],

    tokenizer: {
        // Velocity first, so a `#` or `$` that starts Velocity is never read as markup. A `#` or
        // `$` that starts nothing (`#hashtag`, `$5`) is plain text.
        root: [{ include: '@velocity' }, ...VELOCITY_HTML_ROOT, [/[#$]/, '']],

        ...VELOCITY_HTML_STATES,

        velocity: VELOCITY_ANYWHERE,

        velocityBlockComment: [
            [/\*#/, 'comment.velocity', '@pop'],
            [/[^*]+/, 'comment.velocity'],
            [/\*/, 'comment.velocity']
        ],

        // After `$name`: follow the chain, then hand anything else back to the state that pushed
        // this one.
        velocityReference: [
            ...VELOCITY_REFERENCE_CHAIN,
            [/./, { token: '@rematch', next: '@pop' }]
        ],

        velocityBracedReference: [
            [/\}/, 'variable.velocity', '@pop'],
            [/[a-zA-Z_][\w-]*/, 'variable.velocity'],
            ...VELOCITY_REFERENCE_CHAIN,
            [/[^}]/, 'variable.velocity']
        ],

        velocityArguments: [
            [/\)/, 'delimiter.parenthesis', '@pop'],
            [/\(/, 'delimiter.parenthesis', '@push'],
            ...VELOCITY_EXPRESSION
        ],

        velocityIndex: [[/\]/, 'delimiter.square', '@pop'], ...VELOCITY_EXPRESSION],

        velocityMap: [[/\}/, 'delimiter.curly', '@pop'], ...VELOCITY_EXPRESSION],

        // Double-quoted Velocity strings interpolate references, so those keep their colour.
        velocityStringDouble: [
            [/"/, 'string.velocity', '@pop'],
            [/\$!?\{[^}]*\}/, 'variable.velocity'],
            [/\$!?[a-zA-Z_][\w-]*(?:\.[a-zA-Z_]\w*)*/, 'variable.velocity'],
            [/[^"\\$]+/, 'string.velocity'],
            [/\\./, 'string.escape.velocity'],
            [/[$\\]/, 'string.velocity']
        ],

        velocityStringSingle: [
            [/'/, 'string.velocity', '@pop'],
            [/[^'\\]+/, 'string.velocity'],
            [/\\./, 'string.escape.velocity'],
            [/\\/, 'string.velocity']
        ]
    }
};

/**
 * Registers the Velocity language with Monaco, once.
 *
 * Safe to call as often as needed: it does nothing until Monaco has loaded, and nothing once the
 * language is known. Callers invoke it both when the loader reports Monaco ready and when an editor
 * initialises, because an editor created before the language exists falls back to plain text.
 */
export function ensureDotVelocityLanguageRegistered(): void {
    const monaco = (globalThis as { monaco?: MonacoLanguagesNamespace }).monaco;

    if (!monaco) {
        return;
    }

    const alreadyRegistered = monaco.languages
        .getLanguages()
        .some((language) => language.id === DOT_VELOCITY_LANGUAGE_ID);

    if (alreadyRegistered) {
        return;
    }

    monaco.languages.register({
        id: DOT_VELOCITY_LANGUAGE_ID,
        extensions: ['.vtl'],
        mimetypes: ['text/x-velocity']
    });
    monaco.languages.setMonarchTokensProvider(DOT_VELOCITY_LANGUAGE_ID, DOT_VELOCITY_GRAMMAR);
}
