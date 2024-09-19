import { BlockEditorRenderer } from "@dotcms/react";

const CustomParagraph = ({ content }) => {
    if (!content) {
        return null;
    }
    const [{ text }] = content;
    return <h1 style={{color: 'red'}}>{text}</h1>
}

const ActivityBlock = (props) => {
    const { title, description } = props.attrs.data;

    return (<div>
        <h1>{title}</h1>
        <p>{description}</p>
        </div>)
}


function BlogWithBlockEditor({blockEditorItem}){
    return <BlockEditorRenderer 
        blocks={blockEditorItem} 
        customRenderers={{
            'Activity': ActivityBlock, 
            'paragraph': CustomParagraph 
        }} 
        style={{ backgroundColor: 'lightblue', padding: '10px', fontSize: '40px' }} 
        className="blocks"
    />
}
export default BlogWithBlockEditor;