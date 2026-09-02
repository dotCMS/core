interface WebPageContentProps {
    title?: string;
    body?: string;
}

function WebPageContent({ title, body }: WebPageContentProps) {
    return (
        <section className="mx-auto w-full max-w-2xl">
            {title && (
                <h2 className="mb-4 font-display text-h2 font-semibold text-ink">{title}</h2>
            )}
            {body && (
                <div
                    className="prose prose-lg max-w-none text-ink prose-headings:font-display prose-a:text-primary hover:prose-a:text-primary-deep"
                    dangerouslySetInnerHTML={{ __html: body }}
                />
            )}
        </section>
    );
}

export default WebPageContent;
