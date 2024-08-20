import { BlockEditorRenderer } from "@dotcms/react";

function BlogWithBlockEditor({blockEditorItem}){
    return <>
        <BlockEditorRenderer content={blockEditorItem.content} />
    </>
}
export default BlogWithBlockEditor;