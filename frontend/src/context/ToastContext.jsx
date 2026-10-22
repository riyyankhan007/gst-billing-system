import React, { createContext, useContext, useState, useCallback, useRef } from "react";
import ToastContainer from "../components/ui/Toast";
import { shouldDedupeToast } from "../utils/loadingFeedback";

const ToastContext = createContext(null);

export function ToastProvider({ children }) {
    const [toasts, setToasts] = useState([]);
    const recentToastsRef = useRef(new Map());

    const removeToast = useCallback((id) => {
        setToasts((prev) => prev.filter((t) => t.id !== id));
    }, []);

    const addToast = useCallback(({ message, type = "info", duration = 4000 }) => {
        if (!message) return;

        // Deduplication: prevent identical toast within 1.5 seconds
        const dedupeKey = `${type}:${message}`;
        if (shouldDedupeToast(recentToastsRef.current, dedupeKey)) {
            return;
        }

        const id = "toast_" + now + "_" + Math.random().toString(36).substring(2, 7);
        const newToast = { id, message, type, duration };

        setToasts((prev) => {
            // Keep at most 4 simultaneous toasts
            const trimmed = prev.length >= 4 ? prev.slice(prev.length - 3) : prev;
            return [...trimmed, newToast];
        });

        if (duration > 0) {
            setTimeout(() => {
                removeToast(id);
            }, duration);
        }

        return id;
    }, [removeToast]);

    const success = useCallback((message, duration) => addToast({ message, type: "success", duration }), [addToast]);
    const error = useCallback((message, duration = 5000) => addToast({ message, type: "error", duration }), [addToast]);
    const warning = useCallback((message, duration) => addToast({ message, type: "warning", duration }), [addToast]);
    const info = useCallback((message, duration) => addToast({ message, type: "info", duration }), [addToast]);

    const value = {
        toast: {
            show: addToast,
            success,
            error,
            warning,
            info,
            dismiss: removeToast
        }
    };

    return (
        <ToastContext.Provider value={value}>
            {children}
            <ToastContainer toasts={toasts} onDismiss={removeToast} />
        </ToastContext.Provider>
    );
}

export function useToast() {
    const context = useContext(ToastContext);
    if (!context) {
        // Fallback safe dummy object in case used outside provider
        return {
            show: () => {},
            success: (msg) => console.log("[Toast Success]:", msg),
            error: (msg) => console.error("[Toast Error]:", msg),
            warning: (msg) => console.warn("[Toast Warning]:", msg),
            info: (msg) => console.info("[Toast Info]:", msg),
            dismiss: () => {}
        };
    }
    return context.toast;
}

export default ToastContext;
