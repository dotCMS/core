export const UnknownContentType = ({ contentType }: { contentType: string }) => {
    return (
        <div
            style={{
                backgroundColor: '#f9fafb',
                color: '#1f2937',
                padding: '1rem',
                borderRadius: '0.5rem',
                marginBottom: '1rem',
                border: '1px solid #343a40'
            }}>
            Warning: The content type <strong>{contentType}</strong> is not recognized. Please
            ensure a custom renderer is provided for this content type.
            <br />
            Learn more about how to create a custom renderer in the{' '}
            <a
                href="https://dev.dotcms.com/docs/block-editor"
                target="_blank"
                rel="noopener noreferrer">
                Block Editor Custom Renderers
            </a>
            .
        </div>
    );
};
