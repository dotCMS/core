export const UnknownContentType = ({ contentType }: { contentType: string }) => {
    return (
        <div
            data-testId="unknown-content-type"
            style={{
                backgroundColor: '#fffaf0',
                color: '#333',
                padding: '1rem',
                borderRadius: '0.5rem',
                marginBottom: '1rem',
                marginTop: '1rem',
                border: '1px solid #ed8936'
            }}>
            <strong style={{ color: '#c05621' }}>Dev Warning</strong>: The content type{' '}
            <strong style={{ color: '#c05621' }}>{contentType}</strong> is not recognized. Please
            ensure a custom renderer is provided for this content type.
            <br />
            Learn more about how to create a custom renderer in the{' '}
            <a
                href="https://dev.dotcms.com/docs/block-editor"
                target="_blank"
                rel="noopener noreferrer"
                style={{ color: '#c05621' }}>
                Block Editor Custom Renderers
            </a>
            .
        </div>
    );
};
