import type { DotCMSBasicContentlet } from "@dotcms/types";

export interface ActivityProps extends DotCMSBasicContentlet {
  description: string;
  urlMap?: string;
  urlTitle?: string;
  widgetTitle?: string;
}

function Activity({
  title,
  description,
  image,
  inode,
  urlTitle,
}: ActivityProps) {
  return (
    <article className="p-4 overflow-hidden bg-white rounded-sm shadow-lg mb-4">
      {image && (
        <div className="relative w-full h-56 overflow-hidden">
          <img
            className="w-full h-full object-cover"
            src={`/dA/${inode}`}
            alt="Activity Image"
          />
        </div>
      )}
      <div className="px-6 py-4">
        <p className="mb-2 text-xl font-bold">{title}</p>
        <p className="text-base line-clamp-3">{description}</p>
      </div>
      <div className="px-6 pt-4 pb-2">
        <a
          href={`/activities/${urlTitle || "#"}`}
          className="inline-block px-4 py-2 font-bold rounded-full bg-violet-800 hover:bg-blue-700 text-white"
        >
          Link to detail →
        </a>
      </div>
    </article>
  );
}

export default Activity;
