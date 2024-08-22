import { BlockEditorRenderer } from "@dotcms/react";

const CustomParagraph = () => <h1 style={{color: 'red'}}>HEY</h1>

function BlogWithBlockEditor({blockEditorItem}){
    return <BlockEditorRenderer blocks={blockEditorItem} customRenderers={{'paragraph': CustomParagraph}}  />
    // style={{ backgroundColor: 'lightblue', padding: '10px', fontSize: '40px' }}
    {/* customRenderers={{'paragraph': CustomParagraph}} */}

}
export default BlogWithBlockEditor;