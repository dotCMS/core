import { MonacoEditorConstructionOptions } from '@materia-ui/ngx-monaco-editor';
import { RawEditorOptions } from 'tinymce';

import { SelectItem } from 'primeng/api';

import { DEFAULT_MONACO_CONFIG } from '../../models/dot-edit-content-field.constant';

/**
 * Enum representing the available editors in the application.
 *
 * This enum provides the editor options that can be used within the system,
 * allowing standardized reference to the supported editors.
 *
 * The enum includes the following members:
 * - TinyMCE: Represents the TinyMCE editor.
 * - Monaco: Represents the Monaco editor.
 */
export enum AvailableEditor {
    TinyMCE = 'TinyMCE',
    Monaco = 'Monaco'
}

/**
 * Enum for representing the available languages supported in the Monaco Editor.
 * Each enum value corresponds to a specific language identifier.
 */
export enum AvailableLanguageMonaco {
    PlainText = 'plaintext',
    Javascript = 'javascript',
    Markdown = 'markdown',
    Html = 'html'
}

/**
 * An array of objects representing options for languages available in Monaco Editor.
 *
 * Each element in the array contains a label for display purposes and a corresponding value from the AvailableLanguageMonaco enumeration.
 *
 * The options provided include:
 * - Html
 * - Javascript
 * - Markdown
 * - Plain Text
 *
 * @type {SelectItem[]}
 */
export const MonacoLanguageOptions: SelectItem[] = [
    { label: 'Html', value: AvailableLanguageMonaco.Html },
    { label: 'Javascript', value: AvailableLanguageMonaco.Javascript },
    { label: 'Markdown', value: AvailableLanguageMonaco.Markdown },
    { label: 'Plain Text', value: AvailableLanguageMonaco.PlainText }
];

/**
 * EditorOptions is an array of select items representing the available
 * editor choices for users. Each item includes a label describing
 * the editor type and a value corresponding to the specific available editor.
 *
 * @type {SelectItem[]}
 */
export const EditorOptions: SelectItem[] = [
    { label: 'WYSIWYG', value: AvailableEditor.TinyMCE },
    { label: 'Code', value: AvailableEditor.Monaco }
];

/**
 * The default editor configuration used for WYSIWYG Field.
 */
export const DEFAULT_EDITOR = AvailableEditor.TinyMCE;

/**
 * The default language setting for the Monaco Editor.
 */
export const DEFAULT_MONACO_LANGUAGE = 'html';

/**
 * Default Configuration object for the Monaco Editor used as the default WYSIWYG field.
 */
export const DEFAULT_WYSIWYG_FIELD_MONACO_CONFIG: MonacoEditorConstructionOptions = {
    ...DEFAULT_MONACO_CONFIG,
    language: DEFAULT_MONACO_LANGUAGE,
    automaticLayout: true,
    theme: 'vs'
};

/**
 * The default configuration object for initializing TinyMCE.
 *
 * Properties:
 * - `menubar` (boolean): Controls the visibility of the menubar in the editor.
 * - `image_caption` (boolean): Enables captions for images.
 * - `image_advtab` (boolean): Adds an advanced tab for images in the image dialog.
 * - `contextmenu` (string): Specifies which context menu items are shown.
 * - `toolbar1` (string): Defines the toolbar layout and the buttons included.
 * - `plugins` (string): A comma-separated list of plugins to include in TinyMCE.
 * - `theme` (string): Sets the theme of the TinyMCE editor.
 */
export const DEFAULT_TINYMCE_CONFIG: Partial<RawEditorOptions> = {
    menubar: false,
    contextmenu: 'align link image',
    toolbar1:
        'undo redo | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent dotAddImage hr',
    plugins:
        'advlist autolink lists link image charmap preview anchor pagebreak searchreplace wordcount visualblocks visualchars code fullscreen insertdatetime media nonbreaking save table directionality emoticons template',
    theme: 'silver'
};

/**
 * MdSyntax is an array containing common Markdown syntax elements.
 * These elements include different levels of headings, list item markers,
 * code block delimiters, and blockquote symbols.
 *
 * The array items are:
 * - `# ` for level-1 heading.
 * - `## ` for level-2 heading.
 * - `### ` for level-3 heading.
 * - `- ` for unordered list items.
 * - `* ` for unordered list items.
 * - `1. ` for ordered list items.
 * - `\`\`\`` for code blocks.
 * - `>[` for blockquotes.
 */
export const MdSyntax = ['# ', '## ', '### ', '- ', '* ', '1. ', '```', '>['];

/**
 * HtmlTags is an array containing a list of common HTML tag names.
 *
 * The array includes tags that are frequently used in HTML documents,
 * such as 'div', 'p', 'span', 'a', 'img', 'ul', 'li', and 'table'.
 * Each tag name in the array is a string and represents the opening part of an HTML element.
 *
 * This array can be used to identify or manipulate these specific HTML elements in a web development context.
 */
export const HtmlTags = ['<div', '<p>', '<span', '<a ', '<img', '<ul', '<li', '<table'];

/**
 * An array of common JavaScript keywords and operators.
 *
 * This array includes keywords used for declarations,
 * defining functions, classes, and imports.
 *
 */
export const JsKeywords = [
    'function',
    'const ',
    'let ',
    'var ',
    '=>',
    'class ',
    'interface ',
    'import '
];

/**
 * A string constant representing the default placeholder comment used by TinyMCE editor.
 *
 * This constant is used to identify and differentiate the areas within the content where the TinyMCE
 * WYSIWYG editor is initialized.
 *
 * @constant {string} COMMENT_TINYMCE
 */
export const COMMENT_TINYMCE = '<!--dotcms:wysiwyg-->';
