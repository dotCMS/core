import Link from "next/link";

export function ErrorLayout({ status, heading, body }) {
    return (
        <div className="bg-slate-100 min-h-dvh w-full flex justify-center items-center">
            <section>
                <div className="py-8 px-4 mx-auto max-w-5xl lg:py-16 lg:px-6">
                    <div className="mx-auto max-w-2xl text-center">
                        <h1 className="mb-4 text-7xl tracking-tight font-extrabold lg:text-9xl text-primary-600">
                            {status}
                        </h1>
                        <p className="mb-4 text-3xl tracking-tight font-bold text-gray-900 md:text-4xl">
                            {heading}
                        </p>
                        <p className="mb-4 text-lg font-light text-gray-500">
                            {body}
                        </p>
                        <Link
                            href="/"
                            className="inline-flex text-white bg-purple-600 hover:bg-purple-800 focus:ring-4 focus:outline-hidden focus:ring-purple-300 font-medium rounded-lg text-sm px-5 py-2.5 text-center my-4"
                        >
                            Return Home
                        </Link>
                    </div>
                </div>
            </section>
        </div>
    );
}
