import {
    EXPERIMENT_DB_DEFAULT_VERSION,
    EXPERIMENT_DB_KEY_PATH,
    EXPERIMENT_DB_NAME,
    EXPERIMENT_DB_STORE_NAME
} from '../constants';
import { AssignedExperiments, IsUserIncludedApiResponse } from '../models';

/**
 * Creates or opens a IndexedDB database with the specified version.
 *
 *
 * @returns {Promise<IDBDatabase>} A promise that resolves to the opened database.
 * The promise will be rejected with an error message if there was a database error.
 */
const openDB = (): Promise<IDBDatabase> => {
    return new Promise<IDBDatabase>((resolve, reject) => {
        const request = indexedDB.open(EXPERIMENT_DB_NAME, EXPERIMENT_DB_DEFAULT_VERSION);

        request.onupgradeneeded = (event: IDBVersionChangeEvent) => {
            const db = getRequestResult(event);

            if (!db.objectStoreNames.contains(EXPERIMENT_DB_STORE_NAME)) {
                db.createObjectStore(EXPERIMENT_DB_STORE_NAME);
            }
        };

        request.onerror = (event) => {
            const errorMsj = getOnErrorMessage((event.target as IDBRequest).error);
            reject(errorMsj);
        };

        request.onsuccess = (event: Event) => {
            const db = getRequestResult(event);
            resolve(db);
        };
    });
};

/**
 * Saves the provided data to indexDB.
 *
 * @async
 * @param {AssignedExperiments} data - The data to be saved.
 * @returns {Promise<any>} - The result of the save operation.
 */
export const persistData = async (data: AssignedExperiments): Promise<IDBValidKey> => {
    const db = await openDB();

    return await new Promise((resolve, reject) => {
        const transaction = db.transaction([EXPERIMENT_DB_STORE_NAME], 'readwrite');
        const store = transaction.objectStore(EXPERIMENT_DB_STORE_NAME);
        const request = store.put(data, EXPERIMENT_DB_KEY_PATH);

        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
};

/**
 * Retrieves data from the database using a specific key.
 *
 * @async
 * @returns {Promise<any>} A promise that resolves with the data retrieved from the database.
 */
export const getData = async (): Promise<IsUserIncludedApiResponse['entity']> => {
    const db = await openDB();

    return await new Promise((resolve, reject) => {
        const transaction = db.transaction([EXPERIMENT_DB_STORE_NAME], 'readonly');
        const store = transaction.objectStore(EXPERIMENT_DB_STORE_NAME);
        const request = store.get(EXPERIMENT_DB_KEY_PATH);

        request.onsuccess = () => resolve(request.result as IsUserIncludedApiResponse['entity']);
        request.onerror = () => reject(request.error);
    });
};

/**
 * Retrieves the result of a database request from an Event object.
 *
 * @param {Event} event - The Event object containing the database request.
 * @returns {IDBDatabase} - The result of the database request, casted as an IDBDatabase object.
 */
const getRequestResult = (event: Event): IDBDatabase =>
    (event.target as IDBRequest).result as IDBDatabase;

/**
 * Builds an error message based on the provided error object.
 * @param {DOMException | null} error - The error object to build the message from.
 * @returns {string} The constructed error message.
 */
const getOnErrorMessage = (error: DOMException | null): string => {
    let errorMessage =
        'A database error occurred while using IndexedDB. Your browser may not support IndexedDB or IndexedDB might not be enabled.';

    if (error) {
        errorMessage += ' Details: ';

        if (error.name) {
            errorMessage += ` Error Name: ${error.name}`;
        }

        if (error.message) {
            errorMessage += ` Error Message: ${error.message}`;
        }

        if (error.code) {
            errorMessage += ` Error Code: ${error.code}`;
        }
    }

    return errorMessage;
};
