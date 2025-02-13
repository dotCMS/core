import { HTML_BASE_TOKENIZER_ROOT, HTML_BASE_TOKENIZER_STATES } from './html-monaco-language-base';

const VELOCITY_TOKENS = [
    // Velocity directives with markers
    [/^\*#.*?#$\*$/, 'code.velocity'], // Marked directives *#...#*

    // Velocity comments
    [/#\*[\s\S]*?\*#/, 'comment.velocity'], // Block comments
    [/^\s*##.*/, 'comment.velocity'], // Line comments with optional leading whitespace

    // Velocity directives
    [/#(foreach|if|else|elseif|end|set|parse|include|macro|stop)\b/, 'keyword.velocity'],
    [/#(dotParse)\b/, 'keyword.dotparse.velocity'],

    // Velocity Variables
    [/\$!?\{[^}]+}/, 'variable.velocity'],
    [/\$!?[a-zA-Z_][a-zA-Z0-9_]*/, 'variable.velocity']
] as monaco.languages.IMonarchLanguageRule[];

const VELOCITY_STATES = {
    velocityVariable: [
        [/\}/, 'variable.velocity.delimiter', '@pop'],
        [/\(/, 'delimiter.parenthesis', '@velocityMethod'],
        [/[^}()]/, 'variable.velocity']
    ] as monaco.languages.IMonarchLanguageRule[],

    velocityMethod: [
        [/\)/, 'delimiter.parenthesis', '@pop'],
        [/\(/, 'delimiter.parenthesis', '@push'],
        [/[^()]/, 'variable.velocity']
    ] as monaco.languages.IMonarchLanguageRule[]
};

export const dotVelocityLanguageDefinition: monaco.languages.IMonarchLanguage = {
    defaultToken: '',
    tokenPostfix: '.vtl',
    ignoreCase: true,

    brackets: [
        { open: '{', close: '}', token: 'delimiter.curly' },
        { open: '[', close: ']', token: 'delimiter.square' },
        { open: '(', close: ')', token: 'delimiter.parenthesis' },
        { open: '<', close: '>', token: 'delimiter.angle' }
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
        root: [...VELOCITY_TOKENS, ...HTML_BASE_TOKENIZER_ROOT],
        ...HTML_BASE_TOKENIZER_STATES,
        ...VELOCITY_STATES
    } as monaco.languages.IMonarchLanguage['tokenizer']
};
