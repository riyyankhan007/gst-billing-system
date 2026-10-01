import { useEffect, useState } from "react";
import { getInvoices } from "./services/api";

import CreateInvoice from "./pages/CreateInvoice";
import InvoiceDetails from "./pages/InvoiceDetails";
import Login from "./pages/Login";
import BusinessSettings from "./pages/BusinessSettings";
import ManageData from "./pages/ManageData";

import "./styles/app.css";

function Icon({ type }) {
    const paths = {
        dashboard: (
            <>
                <rect x="3" y="3" width="7" height="7" rx="1" />
                <rect x="14" y="3" width="7" height="7" rx="1" />
                <rect x="3" y="14" width="7" height="7" rx="1" />
                <rect x="14" y="14" width="7" height="7" rx="1" />
            </>
        ),
        invoice: (
            <>
                <path d="M6 3h9l3 3v15H6z" />
                <path d="M15 3v4h4" />
                <path d="M9 12h6M9 16h6" />
            </>
        ),
        customer: (
            <>
                <circle cx="12" cy="8" r="3" />
                <path d="M5 21c.7-3.2 3.1-5 7-5s6.3 1.8 7 5" />
            </>
        ),
        product: (
            <>
                <path d="M4 7h16v13H4z" />
                <path d="M8 7V4h8v3M8 12h8" />
            </>
        ),
        settings: (
            <>
                <circle cx="12" cy="12" r="3" />
                <path d="M19 12a7 7 0 0 0-.1-1l2-1.5-2-3.5-2.3 1a8 8 0 0 0-1.7-1L14.7 3h-4l-.2 2a8 8 0 0 0-1.7 1l-2.3-1-2 3.5L6.5 10a7 7 0 0 0 0 2l-2 1.5 2 3.5 2.3-1a8 8 0 0 0 1.7 1l.2 2h4l.2-2a8 8 0 0 0 1.7-1l2.3 1 2-3.5-2-1.5c.1-.3.1-.7.1-1Z" />
            </>
        ),
        logout: (
            <>
                <path d="M10 4H5v16h5" />
                <path d="M14 8l4 4-4 4M18 12H8" />
            </>
        ),
        plus: (
            <>
                <path d="M12 5v14M5 12h14" />
            </>
        )
    };

    return (
        <svg
            className="icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.7"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            {paths[type]}
        </svg>
    );
}

function App() {
    const [authenticated, setAuthenticated] = useState(
        Boolean(localStorage.getItem("gstToken"))
    );

    const [page, setPage] = useState("dashboard");
    const [invoices, setInvoices] = useState([]);
    const [selectedInvoiceId, setSelectedInvoiceId] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const loadInvoices = async () => {
        try {
            setLoading(true);
            setInvoices(await getInvoices());
            setError("");
        } catch {
            setError("Failed to load invoices");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (authenticated) {
            loadInvoices();
        }
    }, [authenticated]);

    if (!authenticated) {
        return <Login onAuthenticated={() => setAuthenticated(true)} />;
    }

    const logout = () => {
        localStorage.removeItem("gstToken");
        setAuthenticated(false);
    };

    const navigate = (target) => {
        setPage(target);
    };

    const navItem = (target, icon, label) => (
        <button
            className={`sidebar-link ${page === target ? "active" : ""}`}
            onClick={() => navigate(target)}
        >
            <Icon type={icon} />
            <span>{label}</span>
        </button>
    );

    const paidInvoices = invoices.filter(
        (invoice) => invoice.status === "PAID"
    );

    const totalBilled = invoices.reduce(
        (sum, invoice) => sum + Number(invoice.grandTotal || 0),
        0
    );

    if (page === "create") {
        return (
            <AppShell
                page={page}
                navItem={navItem}
                logout={logout}
            >
                <CreateInvoice
                    onBack={() => {
                        setPage("dashboard");
                        loadInvoices();
                    }}
                />
            </AppShell>
        );
    }

    if (page === "details") {
        return (
            <AppShell
                page={page}
                navItem={navItem}
                logout={logout}
            >
                <InvoiceDetails
                    invoiceId={selectedInvoiceId}
                    onBack={() => {
                        setPage("dashboard");
                        loadInvoices();
                    }}
                />
            </AppShell>
        );
    }

    if (page === "customers" || page === "products") {
        return (
            <AppShell
                page={page}
                navItem={navItem}
                logout={logout}
            >
                <ManageData kind={page} />
            </AppShell>
        );
    }

    if (page === "settings") {
        return (
            <AppShell
                page={page}
                navItem={navItem}
                logout={logout}
            >
                <BusinessSettings />
            </AppShell>
        );
    }

    return (
        <AppShell
            page={page}
            navItem={navItem}
            logout={logout}
        >
            <main className="dashboard">
                <div className="page-heading">
                    <div>
                        <span className="eyebrow">OVERVIEW</span>
                        <h1>Business at a glance.</h1>
                        <p>
                            Manage your invoices, customers and products
                            from one place.
                        </p>
                    </div>

                    <button
                        className="primary-button"
                        onClick={() => setPage("create")}
                    >
                        <Icon type="plus" />
                        Create invoice
                    </button>
                </div>

                <section className="stats-grid">
                    <div className="stat">
                        <span>Total billed</span>
                        <strong>
                            ₹{totalBilled.toLocaleString("en-IN", {
                                minimumFractionDigits: 2,
                                maximumFractionDigits: 2
                            })}
                        </strong>
                    </div>

                    <div className="stat">
                        <span>Invoices</span>
                        <strong>{invoices.length}</strong>
                    </div>

                    <div className="stat">
                        <span>Paid invoices</span>
                        <strong>{paidInvoices.length}</strong>
                    </div>
                </section>

                <section className="content-section">
                    <div className="section-heading">
                        <div>
                            <span className="eyebrow">ACTIVITY</span>
                            <h2>Recent invoices</h2>
                        </div>

                        <button
                            className="text-button"
                            onClick={() => setPage("create")}
                        >
                            New invoice
                        </button>
                    </div>

                    {loading && (
                        <div className="empty-state">
                            Loading invoices
                        </div>
                    )}

                    {error && (
                        <div className="error-message">
                            {error}
                        </div>
                    )}

                    {!loading && !error && invoices.length === 0 && (
                        <div className="empty-state">
                            <h3>No invoices yet.</h3>
                            <p>
                                Create your first invoice to get started.
                            </p>

                            <button
                                className="primary-button"
                                onClick={() => setPage("create")}
                            >
                                <Icon type="plus" />
                                Create invoice
                            </button>
                        </div>
                    )}

                    {!loading && !error && invoices.length > 0 && (
                        <div className="invoice-table-wrapper">
                            <table className="invoice-table">
                                <thead>
                                    <tr>
                                        <th>Invoice</th>
                                        <th>Customer</th>
                                        <th>Date</th>
                                        <th>Amount</th>
                                        <th>Status</th>
                                    </tr>
                                </thead>

                                <tbody>
                                    {invoices.map((invoice) => (
                                        <tr
                                            key={invoice.id}
                                            onClick={() => {
                                                setSelectedInvoiceId(
                                                    invoice.id
                                                );
                                                setPage("details");
                                            }}
                                        >
                                            <td className="invoice-number">
                                                {invoice.invoiceNumber}
                                            </td>

                                            <td>
                                                {invoice.customer?.name || "—"}
                                            </td>

                                            <td>
                                                {invoice.invoiceDate || "—"}
                                            </td>

                                            <td>
                                                ₹
                                                {Number(
                                                    invoice.grandTotal || 0
                                                ).toLocaleString("en-IN", {
                                                    minimumFractionDigits: 2
                                                })}
                                            </td>

                                            <td>
                                                <span
                                                    className={`status status-${String(
                                                        invoice.status
                                                    ).toLowerCase()}`}
                                                >
                                                    {invoice.status}
                                                </span>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </section>
            </main>
        </AppShell>
    );
}

function AppShell({ children, navItem, logout }) {
    return (
        <div className="app-shell">
            <aside className="sidebar">
                <div className="brand">
                    <div className="brand-mark">GST</div>
                    <div>
                        <strong>GST / BILLING</strong>
                        <span>Business workspace</span>
                    </div>
                </div>

                <div className="sidebar-section">
                    <span className="sidebar-label">WORKSPACE</span>

                    {navItem("dashboard", "dashboard", "Overview")}
                    {navItem("create", "invoice", "Invoices")}
                    {navItem("customers", "customer", "Customers")}
                    {navItem("products", "product", "Products")}
                </div>

                <div className="sidebar-bottom">
                    <span className="sidebar-label">BUSINESS</span>

                    {navItem("settings", "settings", "Settings")}

                    <button
                        className="sidebar-link"
                        onClick={logout}
                    >
                        <Icon type="logout" />
                        <span>Logout</span>
                    </button>
                </div>
            </aside>

            <div className="main-area">
                <header className="topbar">
                    <span>GST / BILLING</span>
                    <span className="topbar-right">Business workspace</span>
                </header>

                <div className="main-content">
                    {children}
                </div>
            </div>
        </div>
    );
}

export default App;