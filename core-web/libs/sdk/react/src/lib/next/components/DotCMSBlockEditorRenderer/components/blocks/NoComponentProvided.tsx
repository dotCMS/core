export const NoComponentProvided = ({ contentType }: { contentType: string }) => {
    const style = {
        backgroundColor: '#fffaf0',
        color: '#333',
        padding: '1rem',
        borderRadius: '0.5rem',
        marginBottom: '1rem',
        marginTop: '1rem',
        border: '1px solid #ed8936'
    };

    return (
        <div data-testid="no-component-provided" style={style}>
            <strong style={{ color: '#c05621' }}>Dev Warning</strong>: No component or custom
            renderer provided for content type
            <strong style={{ color: '#c05621' }}>{contentType || 'Unknown'}</strong>.
            <br />
            Please refer to the
            <a
                href="https://dev.dotcms.com/docs/block-editor"
                target="_blank"
                rel="noopener noreferrer"
                style={{ color: '#c05621' }}>
                Block Editor Custom Renderers Documentation
            </a>{' '}
            for guidance.
        </div>
    );
};
