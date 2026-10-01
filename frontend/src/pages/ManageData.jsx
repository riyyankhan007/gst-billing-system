import { useEffect, useState } from "react";
import {
    createCustomer,
    createProduct,
    getCustomers,
    getProducts
} from "../services/api";

const customerFields = [
    ["name", "Customer name"],
    ["gstin", "GSTIN"],
    ["address", "Address"],
    ["state", "State"],
    ["stateCode", "State code"],
    ["phone", "Phone"],
    ["email", "Email"]
];

const productFields = [
    ["name", "Product name"],
    ["hsnCode", "HSN code"],
    ["price", "Price"],
    ["gstRate", "GST rate"]
];

export default function ManageData({ kind }) {
    const customer = kind === "customers";
    const fields = customer ? customerFields : productFields;

    const [items, setItems] = useState([]);
    const [form, setForm] = useState({});
    const [error, setError] = useState("");
    const [saving, setSaving] = useState(false);

    const load = async () => {
        try {
            const data = customer
                ? await getCustomers()
                : await getProducts();

            setItems(data);
        } catch (err) {
            setError(err.message || "Failed to load data");
        }
    };

    useEffect(() => {
        setForm({});
        setError("");
        load();
    }, [kind]);

    const submit = async (e) => {
        e.preventDefault();
        setError("");
        setSaving(true);

        try {
            const body = { ...form };

            if (customer) {
                body.stateCode = Number(body.stateCode);
                await createCustomer(body);
            } else {
                body.price = Number(body.price);
                body.gstRate = Number(body.gstRate);
                await createProduct(body);
            }

            setForm({});
            await load();
        } catch (err) {
            setError(err.message || "Failed to save");
        } finally {
            setSaving(false);
        }
    };

    return (
        <main className="management-page">

            <div className="page-intro">
                <div>
                    <span className="eyebrow">
                        {customer ? "CUSTOMERS" : "PRODUCTS"}
                    </span>

                    <h1>
                        {customer
                            ? "Your customers."
                            : "Your products."}
                    </h1>

                    <p>
                        {customer
                            ? "Keep customer details ready for your invoices."
                            : "Manage the products and GST rates you sell."}
                    </p>
                </div>
            </div>

            <div className="management-layout">

                <section className="form-panel">
                    <div className="form-heading">
                        <span className="eyebrow">ADD NEW</span>
                        <h2>
                            {customer
                                ? "Add a customer"
                                : "Add a product"}
                        </h2>
                    </div>

                    <form onSubmit={submit}>

                        {fields.map(([field, label]) => (
                            <div
                                className={
                                    customer && field === "stateCode"
                                        ? "form-field"
                                        : "form-field"
                                }
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
                                            : field === "price" ||
                                              field === "gstRate" ||
                                              field === "stateCode"
                                            ? "number"
                                            : "text"
                                    }
                                    value={form[field] || ""}
                                    onChange={(e) =>
                                        setForm({
                                            ...form,
                                            [field]: e.target.value
                                        })
                                    }
                                    required={
                                        field === "name"
                                    }
                                    placeholder={
                                        field === "gstin"
                                            ? "Enter GSTIN"
                                            : field === "stateCode"
                                            ? "29"
                                            : ""
                                    }
                                />
                            </div>
                        ))}

                        {error && (
                            <p className="form-error">
                                {error}
                            </p>
                        )}

                        <button
                            className="primary-button form-submit"
                            type="submit"
                            disabled={saving}
                        >
                            {saving
                                ? "Saving..."
                                : customer
                                ? "Add customer"
                                : "Add product"}
                        </button>
                    </form>
                </section>

                <section className="data-panel">
                    <div className="data-panel-header">
                        <div>
                            <span className="eyebrow">
                                {customer
                                    ? "CUSTOMER LIST"
                                    : "PRODUCT LIST"}
                            </span>

                            <h2>
                                {items.length}{" "}
                                {customer
                                    ? "customers"
                                    : "products"}
                            </h2>
                        </div>
                    </div>

                    {items.length === 0 ? (
                        <div className="empty-data">
                            <p>
                                No {customer ? "customers" : "products"}{" "}
                                added yet.
                            </p>
                        </div>
                    ) : (
                        <div className="data-list">
                            {items.map((item) => (
                                <div
                                    className="data-row"
                                    key={item.id}
                                >
                                    <div>
                                        <strong>
                                            {item.name}
                                        </strong>

                                        <span>
                                            {customer
                                                ? `${item.state || ""}${
                                                      item.gstin
                                                          ? ` · ${item.gstin}`
                                                          : ""
                                                  }`
                                                : `HSN ${
                                                      item.hsnCode ||
                                                      "—"
                                                  }`}
                                        </span>
                                    </div>

                                    <div className="data-value">
                                        {customer
                                            ? item.phone || "—"
                                            : `₹${Number(
                                                  item.price || 0
                                              ).toLocaleString(
                                                  "en-IN"
                                              )}`}
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </section>

            </div>
        </main>
    );
}