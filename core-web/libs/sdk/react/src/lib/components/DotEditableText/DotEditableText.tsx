import { Editor } from '@tinymce/tinymce-react';
import { useEffect, useRef, useState } from 'react';

import {
    isInsideEditor as isInsideEditorFn,
    postMessageToEditor,
    CUSTOMER_ACTIONS,
    DotCmsClient
} from '@dotcms/client';

import { DotEditableTextProps, TINYMCE_CONFIG } from './utils';

export function DotEditableText({
    mode = 'plain',
    format = 'text',
    contentlet,
    fieldName = ''
}: DotEditableTextProps) {
    const editorRef = useRef<Editor['editor'] | null>(null);
    const [isInsideEditor, setisInsideEditor] = useState(false);
    const [content, setContent] = useState(contentlet[fieldName] || '');

    useEffect(() => {
        const isInsideEditor = isInsideEditorFn();
        setisInsideEditor(isInsideEditor);

        if (!isInsideEditor) {
            return;
        }

        const content = contentlet[fieldName] || '';
        editorRef.current?.setContent(content, { format });
        setContent(content);
    }, [contentlet, fieldName, format]);

    useEffect(() => {
        const onMessage = ({ data }: MessageEvent) => {
            const { name, payload } = data;
            if (name !== 'COPY_CONTENTLET_INLINE_EDITING_SUCCESS') {
                return;
            }

            const { oldInode, inode } = payload;
            const currentInode = contentlet.inode;

            if (currentInode === oldInode || currentInode === inode) {
                editorRef.current?.focus();

                return;
            }
        };

        window.addEventListener('message', onMessage);

        return () => {
            window.removeEventListener('message', onMessage);
        };
    }, [contentlet.inode]);

    const onMouseDown = (event: MouseEvent) => {
        const { onNumberOfPages = 1 } = contentlet;

        if (onNumberOfPages <= 1 || editorRef.current?.hasFocus()) {
            return;
        }

        const { inode, languageId: language } = contentlet;

        event.stopPropagation();
        event.preventDefault();

        try {
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
        } catch (error) {
            console.error('Failed to post message to editor:', error);
        }
    };

    const onFocusOut = () => {
        const editedContent = editorRef.current?.getContent({ format: format }) || '';

        if (!editorRef.current?.isDirty() || !didContentChange(editedContent)) {
            return;
        }

        const { inode, languageId: langId } = contentlet;

        try {
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
        } catch (error) {
            console.error('Failed to post message to editor:', error);
        }
    };

    const didContentChange = (editedContent: string) => {
        return content !== editedContent;
    };

    if (!isInsideEditor) {
        // We can let the user pass the Child Component and create a root to get the HTML for the editor
        return <div dangerouslySetInnerHTML={{ __html: content }} />;
    }

    return (
        <Editor
            tinymceScriptSrc={`${DotCmsClient.dotcmsUrl}/ext/tinymcev7/tinymce.min.js`}
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
