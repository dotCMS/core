import { notFound } from "next/navigation";

export async function ErrorPage({ error }) {
    if (error.status === 404) {
        notFound();
    }

    return (
        <div>
            <h1>Status: {error.status}</h1>
            <h2>Message: {error.message}</h2>
        </div>
    );
}
