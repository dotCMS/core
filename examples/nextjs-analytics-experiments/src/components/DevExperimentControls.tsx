"use client";

import { useEffect, useState } from "react";

const DB_NAME = "dotExperimentStore";
const STORE_NAME = "dotExperimentStore";
const DB_KEY = "running_experiment";
const SESSION_KEY = "experimentAlreadyCheck";

interface StoredVariant {
  name: string;
  url: string;
}

interface StoredExperiment {
  id: string;
  name: string;
  pageUrl: string;
  variant: StoredVariant;
  lookBackWindow: { expireMillis: number; expireTime?: number; value?: string };
  regexs: Record<string, string>;
  runningId: string;
}

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, 1);
    req.onsuccess = (e) => resolve((e.target as IDBOpenDBRequest).result);
    req.onerror = (e) => reject((e.target as IDBOpenDBRequest).error);
    req.onupgradeneeded = (e) => {
      const db = (e.target as IDBOpenDBRequest).result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME);
      }
    };
  });
}

async function readExperiments(): Promise<StoredExperiment[]> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, "readonly");
    const store = tx.objectStore(STORE_NAME);
    const req = store.get(DB_KEY);
    req.onsuccess = () => resolve(req.result ?? []);
    req.onerror = () => reject(req.error);
  });
}

async function writeExperiments(data: StoredExperiment[]): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, "readwrite");
    const store = tx.objectStore(STORE_NAME);
    store.clear().onsuccess = () => {
      const put = store.put(data, DB_KEY);
      put.onsuccess = () => resolve();
      put.onerror = () => reject(put.error);
    };
  });
}

/** Derives variant-1 name from experiment UUID: dotexperiment-{first10hexChars}-variant-1 */
function buildVariant1Name(experimentId: string): string {
  const clean = experimentId.replace(/-/g, "").substring(0, 10);
  return `dotexperiment-${clean}-variant-1`;
}

function buildVariant1Url(pageUrl: string, variant1Name: string): string {
  return `${pageUrl}?variantName=${variant1Name}`;
}

const DEFAULT_VARIANT_NAME = "DEFAULT";

export function DevExperimentControls() {
  const [experiments, setExperiments] = useState<StoredExperiment[]>([]);

  useEffect(() => {
    let cancelled = false;

    async function poll() {
      while (!cancelled) {
        const data = await readExperiments().catch(() => []);
        if (data?.length > 0) {
          if (!cancelled) setExperiments(data);
          return;
        }
        await new Promise((r) => setTimeout(r, 500));
      }
    }

    poll();
    return () => { cancelled = true; };
  }, []);

  const enabled = process.env.NEXT_PUBLIC_SHOW_EXPERIMENT_CONTROLS === "true";

  if (!enabled || experiments.length === 0) {
    return null;
  }

  async function forceVariant(
    experimentId: string,
    variantName: string,
    variantUrl: string
  ) {
    const updated = experiments.map((exp) =>
      exp.id === experimentId
        ? { ...exp, variant: { name: variantName, url: variantUrl } }
        : exp
    );
    await writeExperiments(updated);
    // Keep the "already checked" flag set so the SDK reads from IndexedDB
    // without re-calling isUserIncluded (which could randomly re-assign).
    sessionStorage.setItem(SESSION_KEY, "true");
    const target =
      variantName === DEFAULT_VARIANT_NAME
        ? window.location.pathname
        : variantUrl;
    // eslint-disable-next-line react-hooks/immutability -- intentional full-page navigation for dev variant switch
    window.location.href = target;
  }

  return (
    <div
      style={{
        position: "fixed",
        bottom: 16,
        right: 16,
        background: "#1e293b",
        color: "#f1f5f9",
        borderRadius: 8,
        padding: "12px 16px",
        fontSize: 13,
        zIndex: 9999,
        maxWidth: 320,
        boxShadow: "0 4px 12px rgba(0,0,0,0.4)",
      }}
    >
      <div style={{ fontWeight: 700, marginBottom: 8, color: "#94a3b8" }}>
        DEV: Experiments
      </div>
      {experiments.map((exp) => {
        const variant1Name = buildVariant1Name(exp.id);
        const variant1Url = buildVariant1Url(exp.pageUrl, variant1Name);
        const isDefault = exp.variant.name === DEFAULT_VARIANT_NAME;
        return (
          <div key={exp.id} style={{ marginBottom: 8 }}>
            <div style={{ marginBottom: 4 }}>
              <span style={{ color: "#94a3b8" }}>{exp.name}: </span>
              <span style={{ color: isDefault ? "#fbbf24" : "#34d399", fontWeight: 600 }}>
                {isDefault ? "DEFAULT" : "Variant 1"}
              </span>
            </div>
            <div style={{ display: "flex", gap: 6 }}>
              <button
                disabled={isDefault}
                onClick={() => forceVariant(exp.id, DEFAULT_VARIANT_NAME, exp.pageUrl)}
                style={{
                  flex: 1,
                  padding: "4px 8px",
                  borderRadius: 4,
                  border: "1px solid #475569",
                  background: isDefault ? "#334155" : "#1e293b",
                  color: isDefault ? "#64748b" : "#f1f5f9",
                  cursor: isDefault ? "not-allowed" : "pointer",
                  fontSize: 12,
                }}
              >
                Default
              </button>
              <button
                disabled={!isDefault}
                onClick={() => forceVariant(exp.id, variant1Name, variant1Url)}
                style={{
                  flex: 1,
                  padding: "4px 8px",
                  borderRadius: 4,
                  border: "1px solid #475569",
                  background: !isDefault ? "#334155" : "#1e293b",
                  color: !isDefault ? "#64748b" : "#f1f5f9",
                  cursor: !isDefault ? "not-allowed" : "pointer",
                  fontSize: 12,
                }}
              >
                Variant 1
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );
}
