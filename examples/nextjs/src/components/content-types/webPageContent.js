function WebPageContent({ title, body }) {
    return (
        <>
            <h1 className="text-xl font-bold">{title}</h1>
            <div dangerouslySetInnerHTML={{ __html: body }} />
        </>
    );
}

export default WebPageContent;
