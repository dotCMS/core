import { BlockEditorRenderer } from "@dotcms/react";

const CustomParagraph = () => <h1 style={{color: 'red'}}>HEY</h1>

function BlogWithBlockEditor({blockEditorItem}){
    return <BlockEditorRenderer blocks={blockEditorItem}   />
    {/* customRenderers={{'paragraph': CustomParagraph}} */}

}
export default BlogWithBlockEditor;