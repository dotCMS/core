import Link from "next/link";
import { notFound } from "next/navigation";

const ERROR_COPY = {
    403: {
        heading: "Access Denied",
        body: "Sorry, you don’t have permission to view this page.",
    },
    default: {
        heading: "Something went wrong.",
        body: "An unexpected error occurred. Please try again later.",
    },
};

export async function ErrorPage({ error }) {
    if (error.status === 404) {
        notFound();
    }

    const copy = ERROR_COPY[error.status] ?? ERROR_COPY.default;

    return (
        <div className="bg-slate-100 min-h-dvh w-full flex justify-center items-center">
            <section>
                <div className="py-8 px-4 mx-auto max-w-5xl lg:py-16 lg:px-6">
                    <div className="mx-auto max-w-2xl text-center">
                        <h1 className="mb-4 text-7xl tracking-tight font-extrabold lg:text-9xl text-primary-600">
                            {error.status}
                        </h1>
                        <p className="mb-4 text-3xl tracking-tight font-bold text-gray-900 md:text-4xl">
                            {copy.heading}
                        </p>
                        <p className="mb-4 text-lg font-light text-gray-500">
                            {copy.body}
                        </p>
                        <Link href="/">
                            <div className="inline-flex text-white bg-purple-600 hover:bg-purple-800 focus:ring-4 focus:outline-hidden focus:ring-purple-300 font-medium rounded-lg text-sm px-5 py-2.5 text-center my-4">
                                Return Home
                            </div>
                        </Link>
                    </div>
                </div>
            </section>
        </div>
    );
}
