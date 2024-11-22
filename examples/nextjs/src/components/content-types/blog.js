import { isInsideEditor } from "@dotcms/client";
import { BlockEditorRenderer } from "@dotcms/react";

const CustomParagraph = ({ content }) => {
    if (!content) {
        return null;
    }
    const [{ text }] = content;
    return <p style={{ color: "#333" }}>{text}</p>;
};

const ActivityBlock = (props) => {
    const { title, description } = props.attrs.data;

    return (
        <div>
            <h1>{title}</h1>
            <p>{description}</p>
        </div>
    );
};

function Blog({ blogContent, ...contentlet }) {
    const twActives = isInsideEditor()
        ? "border-2 border-solid border-cyan-400 cursor-pointer"
        : "";

    return (
        <BlockEditorRenderer
            editable={true}
            contentlet={contentlet}
            blocks={blogContent}
            fieldName="blogContent"
            customRenderers={{
                Activity: ActivityBlock,
                paragraph: CustomParagraph,
            }}
            style={{ padding: "10px" }}
            className={`blog-styles ${twActives}`}
        />
    );
}
export default Blog;
