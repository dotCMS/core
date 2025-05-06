import { isEditMode } from '@/utils/isEditMode';
import { DotCMSBlockEditorRenderer } from '@dotcms/react/next';
import { useEffect, useState } from 'react';

const ActivityBlock = (props) => {
    const { title, description } = props.attrs.data;

    return (
        <div>
            <h1>{title}</h1>
            <p>{description}</p>
        </div>
    );
};

function Blog({ blogContent }) {
    const [twActives, setTwActives] = useState("");

    useEffect(() => {
        setTwActives(isEditMode() ? "border-2 border-solid border-cyan-400 cursor-pointer" : "");
    }, []);

    return (
        <DotCMSBlockEditorRenderer
            blocks={blogContent}
            fieldName="blogContent"
            customRenderers={{
                Activity: ActivityBlock
            }}
            className={twActives}
        />
    );
}
export default Blog;
