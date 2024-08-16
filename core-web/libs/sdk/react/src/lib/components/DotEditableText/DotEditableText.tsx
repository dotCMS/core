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
        setisInsideEditor(isInsideEditorFn());

        if (!isInsideEditor) {
            return;
        }
    }, [isInsideEditor]);

    useEffect(() => {
        if (!isInsideEditor) {
            return;
        }

        const content = contentlet[fieldName] || '';
        editorRef.current?.setContent(content, { format });
        setContent(content);
    }, [contentlet, fieldName, format, isInsideEditor]);

    const onMouseDown = (event: MouseEvent) => {
        if (contentlet.onNumberOfPages <= 1 || editorRef.current?.hasFocus()) {
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

    // const innerHTMLToElement = () => {
    //     const element = editorRef.current?.editor.getElement();
    //     const safeHtml = content; // Assuming content is already sanitized
    //     element.innerHTML = safeHtml;
    // };

    const didContentChange = (editedContent: string) => {
        return content !== editedContent;
    };

    return (
        <div>
            {isInsideEditor && (
                <Editor
                    tinymceScriptSrc={`${DotCmsClient.dotcmsUrl}/ext/tinymcev7/tinymce.min.js`}
                    inline={true}
                    onInit={(_, editor) => (editorRef.current = editor)}
                    init={TINYMCE_CONFIG[mode]}
                    initialValue={content}
                    onMouseDown={onMouseDown}
                    onFocusOut={onFocusOut}
                />
            )}
        </div>
    );
}

export default DotEditableText;
