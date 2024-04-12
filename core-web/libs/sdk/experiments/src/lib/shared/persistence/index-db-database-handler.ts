/**
 * Represents the configuration for a database connection.
 * @interface
 */
import {
    EXPERIMENT_ALREADY_CHECKED_KEY,
    EXPERIMENT_FETCH_EXPIRE_TIME_KEY,
    LOCAL_STORAGE_TIME_DURATION_MILLISECONDS
} from '../constants';

/**
 * Represents the configuration for a database connection.
 * @interface
 */
interface DbConfig {
    db_name: string;
    db_store: string;
    db_key_path: string;
}

/**
 * The default version of the database.
 *
 * @type {number}
 * @constant
 */
const DB_DEFAULT_VERSION = 1;

/**
 * The `DatabaseHandler` class offers specific methods to store and get data
 * from IndexedDB.
 *
 * @example
 * // To fetch data from the IndexedDB
 * const data = await DatabaseHandler.getData();
 *
 * @example
 * // To store an object of type AssignedExperiments to IndexedDB
 * await DatabaseHandler.persistData(anAssignedExperiment);
 *
 * @example
 * // To get an object of type AssignedExperiments to IndexedDB
 * await DatabaseHandler.persistData(anAssignedExperiment);
 *
 */

export class IndexDBDatabaseHandler {
    constructor(private config: DbConfig) {
        if (!config) {
            throw new Error('Config is required');
        }

        const { db_name, db_store, db_key_path } = config;

        if (!db_name) {
            throw new Error("'db_name' is required in config");
        }

        if (!db_store) {
            throw new Error("'db_store' is required in config");
        }

        if (!db_key_path) {
            throw new Error("'db_key_path' is required in config");
        }
    }

    /**
     * Saves the provided data to indexDB.
     *
     * @async
     * @param {AssignedExperiments} data - The data to be saved.
     * @returns {Promise<any>} - The result of the save operation.
     */
    public async persistData<T>(data: T): Promise<IDBValidKey> {
        const db = await this.openDB();

        return await new Promise((resolve, reject) => {
            const transaction = db.transaction([this.config.db_store], 'readwrite');

            const store = transaction.objectStore(this.config.db_store);

            const clearRequest = store.clear();

            clearRequest.onerror = () => reject(clearRequest.error);
            clearRequest.onsuccess = () => {
                const request = store.put(data, this.config.db_key_path);

                request.onsuccess = () => resolve(request.result);
                request.onerror = () => reject(request.error);
            };
        });
    }

    /**
     * Retrieves data from the database using a specific key.
     *
     * @async
     * @returns {Promise<any>} A promise that resolves with the data retrieved from the database.
     */
    public async getData<T>(): Promise<T> {
        const db = await this.openDB();

        return await new Promise((resolve, reject) => {
            const transaction = db.transaction([this.config.db_store], 'readonly');

            const store = transaction.objectStore(this.config.db_store);

            const request = store.get(this.config.db_key_path);

            request.onsuccess = () => resolve(request.result as T);
            request.onerror = () => reject(request.error);
        });
    }

    /**
     * Deletes all the data from the IndexedDB store.
     *
     * @async
     * @returns {Promise<void>} - The result of the delete operation.
     */
    public async clearData(): Promise<void> {
        const db = await this.openDB();

        return await new Promise((resolve, reject) => {
            const transaction = db.transaction([this.config.db_store], 'readwrite');

            const store = transaction.objectStore(this.config.db_store);

            const request = store.clear();

            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }

    /**
     * Sets the flag indicating that the experiment has already been checked.
     *
     * @function setFlagExperimentAlreadyChecked
     * @returns {void}
     */
    setFlagExperimentAlreadyChecked(): void {
        sessionStorage.setItem(EXPERIMENT_ALREADY_CHECKED_KEY, 'true');
    }

    /**
     * Sets the fetch expired time in the local storage.
     *
     * @return {void}
     */
    setFetchExpiredTime(): void {
        const expireTime = Date.now() + LOCAL_STORAGE_TIME_DURATION_MILLISECONDS;

        localStorage.setItem(EXPERIMENT_FETCH_EXPIRE_TIME_KEY, expireTime.toString());
    }

    /**
     * Builds an error message based on the provided error object.
     * @param {DOMException | null} error - The error object to build the message from.
     * @returns {string} The constructed error message.
     */
    private getOnErrorMessage(error: DOMException | null): string {
        let errorMessage =
            'A database error occurred while using IndexedDB. Your browser may not support IndexedDB or IndexedDB might not be enabled.';

        if (error) {
            errorMessage += error.name ? ` Error Name: ${error.name}` : '';
            errorMessage += error.message ? ` Error Message: ${error.message}` : '';
            errorMessage += error.code ? ` Error Code: ${error.code}` : '';
        }

        return errorMessage;
    }

    /**
     * Creates or opens a IndexedDB database with the specified version.
     *
     *
     * @returns {Promise<IDBDatabase>} A promise that resolves to the opened database.
     * The promise will be rejected with an error message if there was a database error.
     */
    private openDB(): Promise<IDBDatabase> {
        return new Promise<IDBDatabase>((resolve, reject) => {
            const request = indexedDB.open(this.config.db_name, DB_DEFAULT_VERSION);

            request.onupgradeneeded = (event: IDBVersionChangeEvent) => {
                const db = this.getRequestResult(event);

                if (!db.objectStoreNames.contains(this.config.db_store)) {
                    db.createObjectStore(this.config.db_store);
                }
            };

            request.onerror = (event) => {
                const errorMsj = this.getOnErrorMessage((event.target as IDBRequest).error);

                reject(errorMsj);
            };

            request.onsuccess = (event: Event) => {
                const db = this.getRequestResult(event);

                resolve(db);
            };
        });
    }

    /**
     * Retrieves the result of a database request from an Event object.
     *
     * @param {Event} event - The Event object containing the database request.
     * @returns {IDBDatabase} - The result of the database request, casted as an IDBDatabase object.
     */
    private getRequestResult(event: Event): IDBDatabase {
        return (event.target as IDBRequest).result as IDBDatabase;
    }
}
