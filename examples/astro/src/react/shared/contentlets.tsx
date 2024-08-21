import useImageSrc from "../hooks/useImageSrc";
import Contentlet from "./contentlet";

const dateFormatOptions: DateTimeFormatOptions = {
    year: "numeric",
    month: "long",
    day: "numeric",
};

function Contentlets({ contentlets }) {
    return (
        <ul className="flex flex-col gap-7">
            {contentlets.map((contentlet) => (
                <Contentlet contentlet={contentlet} key={contentlet.identifier}>
                    <li className="flex gap-7 min-h-16">
                        <a
                            className="relative min-w-32"
                            href={contentlet.urlMap || contentlet.url}
                        >
                            {contentlet.image && (
                                <img
                                    src={
                                        useImageSrc({
                                            src: contentlet.image?.idPath ?? contentlet.image
                                        })
                                    }
                                    alt={contentlet.urlTitle}
                                    className="object-cover absolute w-full h-full top-0 left-0"
                                />
                            )}
                        </a>
                        <div className="flex flex-col gap-1">
                            <a
                                className="text-sm text-zinc-900 font-bold"
                                href={contentlet.urlMap || contentlet.url}
                            >
                                {contentlet.title}
                            </a>
                            <time className="text-zinc-600">
                                {new Date(
                                    contentlet.modDate
                                ).toLocaleDateString(
                                    "en-US",
                                    dateFormatOptions
                                )}
                            </time>
                        </div>
                    </li>
                </Contentlet>
            ))}
        </ul>
    );
}

export default Contentlets;
