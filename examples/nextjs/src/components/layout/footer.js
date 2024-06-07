import { useEffect, useState } from "react";
import { client } from "@/app/utils/dotcmsClient";

// Footer component
function Footer() {
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
                console.error("Error fetching blogs", error);
            });
    }, []);

    return (
        <footer className="p-4 text-white bg-purple-500">
            <div className="container mx-auto">
                <h2 className="text-2xl font-bold">Recent Blogs</h2>
                <ul>
                    {blogs.map((blog) => (
                        <li key={blog.identifier}>
                            <a href={`/blog/post/${blog.urlTitle}`}>
                                {blog.title}
                            </a>
                        </li>
                    ))}
                </ul>
            </div>
        </footer>
    );
}

export default Footer;
