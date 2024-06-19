import { useEffect, useState } from "react";
import Contentlets from "@/components/shared/contentlets";
import { client } from "@/utils/dotcmsClient";

export default function Blogs({ pageAsset }) {
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
            })
            .catch((error) => {
                console.error(`Error fetching Blogs`, error);
            });
    }, [pageAsset]); // I need to listen to the pageAsset to re-fetch the blogs when the pageAsset changes

    return (
        <div className="flex flex-col">
            <h2 className="text-2xl font-bold mb-7 text-black">
                Latest Blog Posts
            </h2>
            {!!blogs.length && <Contentlets contentlets={blogs} />}
        </div>
    );
}
