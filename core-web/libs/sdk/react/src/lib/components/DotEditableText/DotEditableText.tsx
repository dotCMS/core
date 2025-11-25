import { Editor } from '@tinymce/tinymce-react';
import { useEffect, useRef, useState } from 'react';

import {
    isInsideEditor as isInsideEditorFn,
    postMessageToEditor,
    CUSTOMER_ACTIONS,
    DotCmsClient
} from '@dotcms/client';

import { DotEditableTextProps, TINYMCE_CONFIG } from './utils';

const MCE_URL = '/ext/tinymcev7/tinymce.min.js';

/**
 * Allows inline edit content pulled from dotCMS API using TinyMCE editor
 *
 * @export
 * @component
 * @param {Readonly<DotEditableTextProps>} props {
 *     mode = 'plain',
 *     format = 'text',
 *     contentlet,
 *     fieldName = ''
 * }
 * @example
 * ```javascript
 * import { DotEditableText } from '@dotcms/react';
 *
 * const MyContentletWithTitle = ({ contentlet }) => (
 *     <h2>
 *         <DotEditableText
 *             contentlet={contentlet}
 *             fieldName="title"
 *             mode='full'
 *             format='text'/>
 *     </h2>
 * );
 * ```
 * @returns {JSX.Element} A component to edit content inline
 */
export function DotEditableText({
    mode = 'plain',
    format = 'text',
    contentlet,
    fieldName = ''
}: Readonly<DotEditableTextProps>): JSX.Element {
    const editorRef = useRef<Editor['editor'] | null>(null);
    const [scriptSrc, setScriptSrc] = useState('');
    const [isInsideEditor, setIsInsideEditor] = useState(false);
    const [content, setContent] = useState(contentlet?.[fieldName] || '');

    useEffect(() => {
        setIsInsideEditor(isInsideEditorFn());

        if (!contentlet || !fieldName) {
            console.error('DotEditableText: contentlet or fieldName is missing');
            console.error('Ensure that all needed props are passed to view and edit the content');

            return;
        }

        if (!isInsideEditorFn()) {
            return;
        }

        const createURL = new URL(MCE_URL, DotCmsClient.dotcmsUrl);
        setScriptSrc(createURL.toString());

        const content = contentlet?.[fieldName] || '';
        editorRef.current?.setContent(content, { format });
        setContent(content);
    }, [format, fieldName, contentlet]);

    useEffect(() => {
        if (!isInsideEditorFn()) {
            return;
        }

        const onMessage = ({ data }: MessageEvent) => {
            const { name, payload } = data;
            if (name !== 'COPY_CONTENTLET_INLINE_EDITING_SUCCESS') {
                return;
            }

            const { oldInode, inode } = payload;
            const currentInode = contentlet.inode;
            const shouldFocus = currentInode === oldInode || currentInode === inode;

            if (shouldFocus) {
                editorRef.current?.focus();
            }
        };

        window.addEventListener('message', onMessage);

        return () => {
            window.removeEventListener('message', onMessage);
        };
    }, [contentlet.inode]);

    const onMouseDown = (event: MouseEvent) => {
        const { onNumberOfPages = 1 } = contentlet;
        const { inode, languageId: language } = contentlet;

        if (onNumberOfPages <= 1 || editorRef.current?.hasFocus()) {
            return;
        }

        event.stopPropagation();
        event.preventDefault();

        postMessageToEditor({
            action: CUSTOMER_ACTIONS.COPY_CONTENTLET_INLINE_EDITING,
            payload: {
                dataset: {
                    inode,
                    language,
                    fieldName
                }
            }
        });
    };

    const onFocusOut = () => {
        const editedContent = editorRef.current?.getContent({ format: format }) || '';
        const { inode, languageId: langId } = contentlet;

        if (!editorRef.current?.isDirty() || !didContentChange(editedContent)) {
            return;
        }

        postMessageToEditor({
            action: CUSTOMER_ACTIONS.UPDATE_CONTENTLET_INLINE_EDITING,
            payload: {
                content: editedContent,
                dataset: {
                    inode,
                    langId,
                    fieldName
                }
            }
        });
    };

    const didContentChange = (editedContent: string) => {
        return content !== editedContent;
    };

    if (!isInsideEditor) {
        // We can let the user pass the Child Component and create a root to get the HTML for the editor
        return <span dangerouslySetInnerHTML={{ __html: content }} />;
    }

    return (
        <Editor
            tinymceScriptSrc={scriptSrc}
            inline={true}
            onInit={(_, editor) => (editorRef.current = editor)}
            init={TINYMCE_CONFIG[mode]}
            initialValue={content}
            onMouseDown={onMouseDown}
            onFocusOut={onFocusOut}
        />
    );
}

export default DotEditableText;
