import { Editor } from '@tinymce/tinymce-react';
import { useEffect, useRef, useState } from 'react';

import { DotCMSBasicContentlet, DotCMSUVEAction, UVE_MODE } from '@dotcms/types';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';
import { sendMessageToUVE, getUVEState } from '@dotcms/uve';
import { __TINYMCE_PATH_ON_DOTCMS__ } from '@dotcms/uve/internal';

import { DotCMSEditableTextProps, TINYMCE_CONFIG } from './utils';

/**
 * Allows inline edit content pulled from dotCMS API using TinyMCE editor
 *
 * @export
 * @component
 * @param {Readonly<DotCMSEditableTextProps>} props {
 *     mode = 'plain',
 *     format = 'text',
 *     contentlet,
 *     fieldName = ''
 * }
 * @example
 * ```javascript
 * import { DotCMSEditableText } from '@dotcms/react';
 *
 * const MyContentletWithTitle = ({ contentlet }) => (
 *     <h2>
 *         <DotCMSEditableText
 *             contentlet={contentlet}
 *             fieldName="title"
 *             mode='full'
 *             format='text'/>
 *     </h2>
 * );
 * ```
 * @returns {JSX.Element} A component to edit content inline
 */
export function DotCMSEditableText<T extends DotCMSBasicContentlet>({
    mode = 'plain',
    format = 'text',
    contentlet,
    fieldName
}: Readonly<DotCMSEditableTextProps<T>>) {
    const editorRef = useRef<Editor['editor'] | null>(null);
    const [scriptSrc, setScriptSrc] = useState('');
    const [initEditor, setInitEditor] = useState(false);
    const [content, setContent] = useState(contentlet?.[fieldName] || '');

    useEffect(() => {
        setContent(contentlet?.[fieldName] || '');
    }, [fieldName, contentlet]);

    useEffect(() => {
        const state = getUVEState();

        setInitEditor(state?.mode === UVE_MODE.EDIT && !!state?.dotCMSHost?.length);

        if (!contentlet || !fieldName) {
            console.error(
                '[DotCMSEditableText]: contentlet or fieldName is missing',
                'Ensure that all needed props are passed to view and edit the content'
            );

            return;
        }

        if (state && state.mode !== UVE_MODE.EDIT) {
            console.warn('[DotCMSEditableText]: TinyMCE is not available in the current mode');

            return;
        }

        if (!state?.dotCMSHost) {
            console.warn(
                '[DotCMSEditableText]: The `dotCMSHost` parameter is not defined. Check that the UVE is sending the correct parameters.'
            );

            return;
        }

        const createURL = new URL(__TINYMCE_PATH_ON_DOTCMS__, state.dotCMSHost);
        setScriptSrc(createURL.toString());

        const content = (contentlet?.[fieldName] as string) || '';

        editorRef.current?.setContent(content, { format });
    }, [format, fieldName, contentlet, content]);

    useEffect(() => {
        if (getUVEState()?.mode !== UVE_MODE.EDIT) {
            return;
        }

        const onMessage = ({ data }: MessageEvent) => {
            const { name, payload } = data;
            if (name !== __DOTCMS_UVE_EVENT__.UVE_COPY_CONTENTLET_INLINE_EDITING_SUCCESS) {
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
    }, [contentlet?.inode]);

    const onMouseDown = (event: MouseEvent) => {
        const { onNumberOfPages = 1 } = contentlet;
        const { inode, languageId: language } = contentlet;

        if (Number(onNumberOfPages) <= 1 || editorRef.current?.hasFocus()) {
            return;
        }

        event.stopPropagation();
        event.preventDefault();

        sendMessageToUVE({
            action: DotCMSUVEAction.COPY_CONTENTLET_INLINE_EDITING,
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

        sendMessageToUVE({
            action: DotCMSUVEAction.UPDATE_CONTENTLET_INLINE_EDITING,
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

    if (!initEditor) {
        // We can let the user pass the Child Component and create a root to get the HTML for the editor
        return <span dangerouslySetInnerHTML={{ __html: content }} />;
    }

    return (
        <div
            style={{
                outline: '2px solid #006ce7',
                borderRadius: '4px'
            }}>
            <Editor
                tinymceScriptSrc={scriptSrc}
                inline={true}
                onInit={(_, editor) => (editorRef.current = editor)}
                init={TINYMCE_CONFIG[mode]}
                initialValue={content as string}
                onMouseDown={onMouseDown}
                onFocusOut={onFocusOut}
            />
        </div>
    );
}

export default DotCMSEditableText;
