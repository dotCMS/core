interface WebPageContentProps {
  title?: string;
  body?: string;
}

export default function WebPageContent({ title, body }: WebPageContentProps) {
  return (
    <section className="prose max-w-none p-4">
      {title && <h1 className="text-2xl font-bold">{title}</h1>}
      {body && <div dangerouslySetInnerHTML={{ __html: body }} />}
    </section>
  );
}
