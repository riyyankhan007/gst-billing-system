import { useEffect, useState } from "react";
import {
    getBusiness,
    updateBusiness,
    uploadLogo
} from "../services/api";

const fields = [
    ["name", "Business name"],
    ["gstin", "GSTIN"],
    ["address", "Address"],
    ["state", "State"],
    ["stateCode", "State code"],
    ["phone", "Phone"],
    ["email", "Email"],
    ["website", "Website"],
    ["invoicePrefix", "Invoice prefix"]
];

export default function BusinessSettings() {
    const [business, setBusiness] = useState(null);
    const [message, setMessage] = useState("");
    const [saving, setSaving] = useState(false);
    const [uploading, setUploading] = useState(false);

    useEffect(() => {
        getBusiness()
            .then(setBusiness)
            .catch(() =>
                setMessage("Could not load business")
            );
    }, []);

    if (!business) {
        return (
            <div className="loading-page">
                Loading business...
            </div>
        );
    }

    const change = (e) => {
        setBusiness({
            ...business,
            [e.target.name]: e.target.value
        });
    };

    const save = async (e) => {
        e.preventDefault();
        setSaving(true);
        setMessage("");

        try {
            const updated = {
                ...business,
                stateCode: Number(business.stateCode)
            };

            setBusiness(await updateBusiness(updated));
            setMessage("Business settings saved.");
        } catch (err) {
            setMessage(
                err.message || "Could not save settings"
            );
        } finally {
            setSaving(false);
        }
    };

    const handleLogo = async (e) => {
        const file = e.target.files?.[0];

        if (!file) {
            return;
        }

        setUploading(true);
        setMessage("");

        try {
            setBusiness(await uploadLogo(file));
            setMessage("Logo uploaded.");
        } catch (err) {
            setMessage(
                err.message || "Could not upload logo"
            );
        } finally {
            setUploading(false);
        }
    };

    return (
        <main className="settings-page">

            <div className="page-intro">
                <span className="eyebrow">
                    BUSINESS
                </span>

                <h1>Your business.</h1>

                <p>
                    Manage the information that appears
                    on your GST invoices.
                </p>
            </div>

            <div className="settings-layout">

                <section className="settings-form">

                    <div className="form-heading">
                        <span className="eyebrow">
                            BUSINESS INFORMATION
                        </span>

                        <h2>Business details</h2>
                    </div>

                    <form onSubmit={save}>

                        {fields.map(([field, label]) => (
                            <div
                                className="form-field"
                                key={field}
                            >
                                <label htmlFor={field}>
                                    {label}
                                </label>

                                <input
                                    id={field}
                                    name={field}
                                    type={
                                        field === "email"
                                            ? "email"
                                            : field === "stateCode"
                                            ? "number"
                                            : "text"
                                    }
                                    value={
                                        business[field] || ""
                                    }
                                    onChange={change}
                                />
                            </div>
                        ))}

                        <button
                            className="primary-button form-submit"
                            disabled={saving}
                        >
                            {saving
                                ? "Saving..."
                                : "Save changes"}
                        </button>

                    </form>

                    {message && (
                        <p className="settings-message">
                            {message}
                        </p>
                    )}
                </section>

                <aside className="logo-panel">

                    <span className="eyebrow">
                        BRANDING
                    </span>

                    <h2>Invoice logo</h2>

                    <div className="logo-preview">
                        {business.logo ? (
                            <img
                                src={business.logo}
                                alt="Business logo"
                            />
                        ) : (
                            <span>
                                No logo
                            </span>
                        )}
                    </div>

                    <label className="upload-button">
                        {uploading
                            ? "Uploading..."
                            : "Upload logo"}

                        <input
                            type="file"
                            accept="image/*"
                            onChange={handleLogo}
                            disabled={uploading}
                        />
                    </label>

                    <p>
                        PNG, JPG or WebP.
                        This logo can appear on your
                        generated invoices.
                    </p>

                </aside>

            </div>

        </main>
    );
}