import { useEffect, useState } from "react";
import { client } from "@/app/utils/dotcmsClient";
import Image from "next/image";

export default function Blogs() {
    const options = {
        year: "numeric",
        month: "long",
        day: "numeric",
    };

    const [blogs, setBlogs] = useState([]);

    useEffect(() => {
        client.content
            .getCollection("Blog")
            .sortBy([
                {
                    field: "modDate",
                    order: "desc",
                },
            ])
            .limit(3)
            .then((response) => {
                setBlogs(response.contentlets);

                console.log(response.contentlets);
            })
            .catch((error) => {
                console.error("Error fetching blogs", error);
            });
    }, []);

    return (
        <div className="flex flex-col">
            <h2 className="text-2xl font-bold mb-7">Latest blog post</h2>
            <ul className="flex flex-col gap-7">
                {blogs.map((blog) => (
                    <li key={blog.identifier} className="flex gap-7 min-h-16">
                        <div className="relative min-w-32">
                            <Image
                                src={`${process.env.NEXT_PUBLIC_DOTCMS_HOST}${
                                    blog.image
                                }?language_id=${blog.languageId || 1}`}
                                alt={blog.urlTitle}
                                fill={true}
                                className="object-cover"
                            />
                        </div>
                        <div className="flex flex-col gap-1">
                            <a
                                className="text-sm text-yellow-200 font-bold"
                                href={blog.urlMap}
                            >
                                {blog.title}
                            </a>
                            <time>
                                {new Date(blog.publishDate).toLocaleDateString(
                                    "en-US",
                                    options
                                )}
                            </time>
                        </div>
                    </li>
                ))}
            </ul>
        </div>
    );
}
