import { BlockEditorRenderer } from "@dotcms/react";

const CustomParagraph = ({content}) => {
    const [{ text }] = content;
    return <h1 style={{color: 'red'}}>{text}</h1>
}

const ActivityBlock = (data) => {
    const { title, description } = data;
    console.log("Activity props => ",data);

    return (<div>
        <h1>{title}</h1>
        <p>{description}</p>
        </div>)
}


function BlogWithBlockEditor({blockEditorItem}){
    return <BlockEditorRenderer blocks={blockEditorItem} customRenderers={{'Activity': ActivityBlock, 'paragraph': CustomParagraph}} style={{ backgroundColor: 'lightblue', padding: '10px', fontSize: '40px' }}/>
    // style={{ backgroundColor: 'lightblue', padding: '10px', fontSize: '40px' }}
    {/* customRenderers={{'paragraph': CustomParagraph}} */}

}
export default BlogWithBlockEditor;