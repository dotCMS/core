
import { DotCMSBlockEditorRenderer } from '@dotcms/react/next';

function Blog({ blogContent }) {
    return (
        <DotCMSBlockEditorRenderer
            blocks={blogContent}
            fieldName="blogContent"
        />
    );
}
export default Blog;
