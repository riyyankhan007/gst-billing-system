import { useEffect, useState } from "react";
import { getInvoices, getProfile, downloadInvoicePdf } from "./services/api";

import Dashboard from "./pages/Dashboard";
import CreateInvoice from "./pages/CreateInvoice";
import InvoiceDetails from "./pages/InvoiceDetails";
import Payments from "./pages/Payments";
import Notes from "./pages/Notes";
import Purchases from "./pages/Purchases";
import Suppliers from "./pages/Suppliers";
import Inventory from "./pages/Inventory";
import ManageData from "./pages/ManageData";
import Reports from "./pages/Reports";
import AuditLogs from "./pages/AuditLogs";
import BusinessSettings from "./pages/BusinessSettings";
import Login from "./pages/Login";
import LandingPage from "./pages/LandingPage";

import "./styles/app.css";

function Icon({ type }) {
    const icons = {
        dashboard: (
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="3" width="7" height="7" rx="1" />
                <rect x="14" y="3" width="7" height="7" rx="1" />
                <rect x="3" y="14" width="7" height="7" rx="1" />
                <rect x="14" y="14" width="7" height="7" rx="1" />
            </svg>
        ),
        invoice: (
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
                <line x1="16" y1="13" x2="8" y2="13" />
                <line x1="16" y1="17" x2="8" y2="17" />
                <line x1="10" y1="9" x2="8" y2="9" />
            </svg>
        ),
        payment: (
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="1" y="4" width="22" height="16" rx="2" ry="2" />
                <line x1="1" y1="10" x2="23" y2="10" />
            </svg>
        ),
        notes: (
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
                <line x1="9" y1="15" x2="15" y2="15" />
            </svg>
        ),
        purchase: (
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="9" cy="21" r="1" />
                <circle cx="20" cy="21" r="1" />
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
            </svg>
        ),
        supplier: (
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="1" y="3" width="15" height="13" />
                <polygon points="16 8 20 8 23 11 23 16 16 16 16 8" />
                <circle cx="5.5" cy="18.5" r="2.5" />
                <circle cx="18.5" cy="18.5" r="2.5" />
            </svg>
        ),
        customer: (
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                <path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
        ),
        product: (
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="16.5" y1="9.4" x2="7.5" y2="4.21" />
                <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
                <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
                <line x1="12" y1="22.08" x2="12" y2="12" />
            </svg>
        ),
        inventory: (
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
                <path d="M3.27 6.96L12 12.01l8.73-5.05" />
                <path d="M12 22.08V12" />
            </svg>
        ),
        report: (
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="18" y1="20" x2="18" y2="10" />
                <line x1="12" y1="20" x2="12" y2="4" />
                <line x1="6" y1="20" x2="6" y2="14" />
            </svg>
        ),
        audit: (
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            </svg>
        ),
        settings: (
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="3" />
                <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
            </svg>
        ),
        logout: (
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                <polyline points="16 17 21 12 16 7" />
                <line x1="21" y1="12" x2="9" y2="12" />
            </svg>
        ),
        plus: (
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
        ),
        whatsapp: (
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
            </svg>
        )
    };

    return icons[type] || null;
}

function App() {
    const [authenticated, setAuthenticated] = useState(
        Boolean(localStorage.getItem("gstToken"))
    );

    const [profile, setProfile] = useState(null);
    const [page, setPage] = useState("dashboard");
    const [invoices, setInvoices] = useState([]);
    const [selectedInvoiceId, setSelectedInvoiceId] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [searchQuery, setSearchQuery] = useState("");
    const [statusFilter, setStatusFilter] = useState("ALL");
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [authView, setAuthView] = useState("landing"); // "landing" | "login" | "register"

    // Listen to token expiration events
    useEffect(() => {
        function handleExpired() {
            setAuthenticated(false);
            setProfile(null);
        }
        window.addEventListener("auth-expired", handleExpired);
        return () => window.removeEventListener("auth-expired", handleExpired);
    }, []);

    const loadProfile = async () => {
        try {
            const data = await getProfile();
            setProfile(data);
        } catch {
            // Profile load failed, token might be expired
        }
    };

    const loadInvoices = async () => {
        try {
            setLoading(true);
            const data = await getInvoices();
            setInvoices(data || []);
            setError("");
        } catch (err) {
            setError(err.message || "Failed to load invoices");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (authenticated) {
            loadProfile();
            loadInvoices();
        }
    }, [authenticated]);

    if (!authenticated) {
        if (authView === "landing") {
            return (
                <LandingPage
                    onLogin={() => setAuthView("login")}
                    onRegister={() => setAuthView("register")}
                />
            );
        }
        return (
            <Login
                initialMode={authView === "register" ? "register" : "login"}
                onAuthenticated={() => setAuthenticated(true)}
                onBackToHome={() => setAuthView("landing")}
            />
        );
    }

    const logout = () => {
        localStorage.removeItem("gstToken");
        setAuthenticated(false);
        setProfile(null);
        setSidebarOpen(false);
    };

    const navigate = (target) => {
        setPage(target);
        setSidebarOpen(false);
        window.scrollTo({ top: 0, behavior: "smooth" });
    };

    const navItem = (target, icon, label) => (
        <button
            type="button"
            className={`sidebar-link ${page === target ? "active" : ""}`}
            onClick={() => navigate(target)}
        >
            <Icon type={icon} />
            <span>{label}</span>
        </button>
    );

    // Filtered invoices
    const filteredInvoices = invoices.filter(inv => {
        const matchesStatus =
            statusFilter === "ALL" ||
            inv.status === statusFilter;

        const q = searchQuery.toLowerCase();
        const matchesQuery =
            !searchQuery ||
            (inv.invoiceNumber && inv.invoiceNumber.toLowerCase().includes(q)) ||
            (inv.customer?.name && inv.customer.name.toLowerCase().includes(q));

        return matchesStatus && matchesQuery;
    });

    const formatCurrency = (amt) =>
        `₹${Number(amt || 0).toLocaleString("en-IN", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        })}`;

    const handleQuickPdfDownload = async (e, id, invoiceNum) => {
        e.stopPropagation();
        try {
            const blob = await downloadInvoicePdf(id);
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = `Invoice-${invoiceNum || id}.pdf`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        } catch (err) {
            alert("Could not download PDF: " + err.message);
        }
    };

    const handleQuickRemind = (e, inv) => {
        e.stopPropagation();
        setSelectedInvoiceId(inv.id);
        setPage("details");
    };

    return (
        <div className="app-shell">
            {/* Mobile Header Bar */}
            <header className="mobile-topbar">
                <button
                    type="button"
                    className="hamburger-btn"
                    onClick={() => setSidebarOpen(true)}
                    aria-label="Open Navigation Menu"
                >
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <line x1="3" y1="12" x2="21" y2="12" />
                        <line x1="3" y1="6" x2="21" y2="6" />
                        <line x1="3" y1="18" x2="21" y2="18" />
                    </svg>
                </button>

                <div className="mobile-brand">
                    <span className="badge">GST</span>
                    <span>{profile?.businessName || "Billing System"}</span>
                </div>

                <button
                    type="button"
                    className="action-btn-sm"
                    onClick={() => navigate("create")}
                    title="New Invoice"
                >
                    + Invoice
                </button>
            </header>

            {/* Backdrop for mobile sidebar */}
            <div
                className={`sidebar-overlay ${sidebarOpen ? "active" : ""}`}
                onClick={() => setSidebarOpen(false)}
            />

            {/* Sidebar */}
            <aside className={`sidebar ${sidebarOpen ? "open" : ""}`}>
                <div className="sidebar-brand">
                    <div className="sidebar-logo-icon">GST</div>
                    <div className="sidebar-brand-text">
                        <strong>{profile?.businessName || "GST Billing"}</strong>
                        <span>Multi-Tenant SaaS</span>
                    </div>
                    <button
                        type="button"
                        className="sidebar-close-btn"
                        onClick={() => setSidebarOpen(false)}
                        aria-label="Close sidebar"
                    >
                        ✕
                    </button>
                </div>

                <nav className="sidebar-nav">
                    <span className="sidebar-group-label">OVERVIEW</span>
                    {navItem("dashboard", "dashboard", "Dashboard")}

                    <span className="sidebar-group-label" style={{ marginTop: "14px" }}>SALES & BILLING</span>
                    {navItem("invoices", "invoice", "Invoices")}
                    {navItem("create", "plus", "Create Invoice")}
                    {navItem("payments", "payment", "Payments Received")}
                    {navItem("notes", "notes", "Credit / Debit Notes")}

                    <span className="sidebar-group-label" style={{ marginTop: "14px" }}>PURCHASES</span>
                    {navItem("purchases", "purchase", "Inward Bills (ITC)")}
                    {navItem("suppliers", "supplier", "Suppliers / Vendors")}

                    <span className="sidebar-group-label" style={{ marginTop: "14px" }}>INVENTORY & MASTER</span>
                    {navItem("products", "product", "Products & Services")}
                    {navItem("customers", "customer", "Customers & Ledger")}
                    {navItem("inventory", "inventory", "Stock Audit Trail")}

                    <span className="sidebar-group-label" style={{ marginTop: "14px" }}>INSIGHTS & AUDIT</span>
                    {navItem("reports", "report", "GST & Sales Reports")}
                    {navItem("audit", "audit", "System Audit Logs")}

                    <span className="sidebar-group-label" style={{ marginTop: "14px" }}>ADMINISTRATION</span>
                    {navItem("settings", "settings", "Settings & Team")}
                </nav>

                <div className="sidebar-footer">
                    {profile && (
                        <div className="sidebar-user-pill">
                            <div className="sidebar-avatar">
                                {profile.name ? profile.name.charAt(0).toUpperCase() : "U"}
                            </div>
                            <div className="sidebar-user-info">
                                <div className="sidebar-user-name" style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                                    <span>{profile.name}</span>
                                    {profile.role && (
                                        <span className="status-badge" style={{ fontSize: "9px", padding: "1px 5px", textTransform: "uppercase" }}>
                                            {profile.role}
                                        </span>
                                    )}
                                </div>
                                <div className="sidebar-user-email">{profile.email}</div>
                            </div>
                        </div>
                    )}

                    <button
                        type="button"
                        className="sidebar-link logout-btn"
                        onClick={logout}
                    >
                        <Icon type="logout" />
                        <span>Sign Out</span>
                    </button>
                </div>
            </aside>

            {/* Main Area */}
            <div className="main-area">
                {/* Desktop Topbar */}
                <header className="topbar">
                    <div className="topbar-left">
                        <span style={{ fontWeight: 600 }}>GST Billing & SaaS ERP</span>
                        {profile?.businessName && (
                            <span className="topbar-badge">{profile.businessName}</span>
                        )}
                        {profile?.role && (
                            <span className="status-badge status-issued" style={{ fontSize: "11px", padding: "2px 8px" }}>
                                Role: {profile.role}
                            </span>
                        )}
                    </div>
                    <div className="topbar-right">
                        {page !== "create" && page !== "create-invoice" && page !== "invoices" && (
                            <button
                                type="button"
                                className="primary-button"
                                onClick={() => navigate("create")}
                            >
                                <Icon type="plus" />
                                <span>Create Invoice</span>
                            </button>
                        )}
                    </div>
                </header>

                {/* Main Content Router */}
                <main className="main-content">
                    {/* Dashboard */}
                    {page === "dashboard" && (
                        <Dashboard
                            onNavigate={navigate}
                            onSelectInvoice={(id) => {
                                setSelectedInvoiceId(id);
                                setPage("details");
                            }}
                        />
                    )}

                    {/* Create Invoice */}
                    {(page === "create" || page === "create-invoice") && (
                        <CreateInvoice
                            onBack={() => {
                                setPage("invoices");
                                loadInvoices();
                            }}
                            onCreated={(id) => {
                                setSelectedInvoiceId(id);
                                setPage("details");
                                loadInvoices();
                            }}
                            onNavigate={navigate}
                        />
                    )}

                    {/* Invoice Details */}
                    {page === "details" && (
                        <InvoiceDetails
                            invoiceId={selectedInvoiceId}
                            onBack={() => {
                                setPage("invoices");
                                loadInvoices();
                            }}
                        />
                    )}

                    {/* Invoices List */}
                    {page === "invoices" && (
                        <div className="dashboard-page">
                            <div className="page-heading">
                                <div>
                                    <span className="eyebrow">SALES MANAGEMENT</span>
                                    <h1>Tax Invoices</h1>
                                    <p>Manage, issue, track payment receipts, and send WhatsApp/Email reminders to customers.</p>
                                </div>

                                <button
                                    type="button"
                                    className="primary-button"
                                    onClick={() => navigate("create")}
                                >
                                    <Icon type="plus" />
                                    <span>Create Invoice</span>
                                </button>
                            </div>

                            <section className="content-section">
                                <div className="section-heading">
                                    <div className="table-toolbar" style={{ width: "100%", justifyContent: "space-between", flexWrap: "wrap", gap: "12px" }}>
                                        {/* Status filter tabs */}
                                        <div style={{ display: "flex", gap: "6px", flexWrap: "wrap" }}>
                                            {["ALL", "DRAFT", "ISSUED", "SENT", "PARTIALLY_PAID", "PAID", "CANCELLED", "OVERDUE"].map(st => (
                                                <button
                                                    key={st}
                                                    type="button"
                                                    className={`action-btn-sm ${statusFilter === st ? "primary-button" : ""}`}
                                                    style={statusFilter === st ? { padding: "6px 12px", minHeight: "auto" } : {}}
                                                    onClick={() => setStatusFilter(st)}
                                                >
                                                    {st === "ALL" ? "All" : st.replace("_", " ")}
                                                </button>
                                            ))}
                                        </div>

                                        {/* Search */}
                                        <div className="search-input-wrapper">
                                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <circle cx="11" cy="11" r="8" />
                                                <line x1="21" y1="21" x2="16.65" y2="16.65" />
                                            </svg>
                                            <input
                                                type="text"
                                                className="search-input"
                                                placeholder="Search customer / invoice..."
                                                value={searchQuery}
                                                onChange={e => setSearchQuery(e.target.value)}
                                            />
                                        </div>
                                    </div>
                                </div>

                                {loading ? (
                                    <div className="empty-state">
                                        <p>Loading invoice records...</p>
                                    </div>
                                ) : error ? (
                                    <div className="empty-state" style={{ color: "var(--danger)" }}>
                                        <p>{error}</p>
                                    </div>
                                ) : invoices.length === 0 ? (
                                    <div className="empty-state">
                                        <h3>No invoices generated yet</h3>
                                        <p>Create your first GST compliant invoice to begin tracking payments and receivables.</p>
                                        <button
                                            type="button"
                                            className="primary-button"
                                            onClick={() => navigate("create")}
                                        >
                                            <Icon type="plus" />
                                            <span>Create First Invoice</span>
                                        </button>
                                    </div>
                                ) : filteredInvoices.length === 0 ? (
                                    <div className="empty-state">
                                        <h3>No matching invoices found</h3>
                                        <p>Try clearing your search query or choosing another status filter.</p>
                                    </div>
                                ) : (
                                    <div className="table-responsive">
                                        <table className="invoice-table">
                                            <thead>
                                                <tr>
                                                    <th>Invoice No</th>
                                                    <th>Customer Name</th>
                                                    <th>Date</th>
                                                    <th>Due Date</th>
                                                    <th style={{ textAlign: "right" }}>Grand Total</th>
                                                    <th style={{ textAlign: "right" }}>Balance Due</th>
                                                    <th style={{ textAlign: "center" }}>Status</th>
                                                    <th style={{ textAlign: "right" }}>Actions</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {filteredInvoices.map(inv => (
                                                    <tr
                                                        key={inv.id}
                                                        onClick={() => {
                                                            setSelectedInvoiceId(inv.id);
                                                            setPage("details");
                                                        }}
                                                    >
                                                        <td className="invoice-number-cell">
                                                            {inv.invoiceNumber}
                                                        </td>
                                                        <td>
                                                            <strong>{inv.customer?.name || "—"}</strong>
                                                            {inv.customer?.gstin && (
                                                                <span style={{ display: "block", fontSize: "11px", color: "var(--text-muted)" }}>
                                                                    GST: {inv.customer.gstin}
                                                                </span>
                                                            )}
                                                        </td>
                                                        <td>{inv.invoiceDate || "—"}</td>
                                                        <td>{inv.dueDate || "—"}</td>
                                                        <td className="amount-cell" style={{ textAlign: "right" }}>
                                                            {formatCurrency(inv.grandTotal)}
                                                        </td>
                                                        <td style={{ textAlign: "right", color: Number(inv.balanceAmount || inv.grandTotal) > 0 ? "var(--warning)" : "var(--success)", fontWeight: 600 }}>
                                                            {formatCurrency(inv.balanceAmount != null ? inv.balanceAmount : inv.grandTotal)}
                                                        </td>
                                                        <td style={{ textAlign: "center" }}>
                                                            <span className={`status-badge status-${String(inv.status).toLowerCase()}`}>
                                                                {inv.status}
                                                            </span>
                                                        </td>
                                                        <td style={{ textAlign: "right" }}>
                                                            <div className="table-actions" style={{ justifyContent: "flex-end" }}>
                                                                {/* Remind button for unpaid invoices */}
                                                                {inv.status !== "PAID" && inv.status !== "CANCELLED" && (
                                                                    <button
                                                                        type="button"
                                                                        className="action-btn-sm remind-btn"
                                                                        title="Send Reminder via WhatsApp or Email"
                                                                        onClick={(e) => handleQuickRemind(e, inv)}
                                                                    >
                                                                        <Icon type="whatsapp" />
                                                                        <span>Remind</span>
                                                                    </button>
                                                                )}

                                                                <button
                                                                    type="button"
                                                                    className="action-btn-sm"
                                                                    title="Download PDF"
                                                                    onClick={(e) => handleQuickPdfDownload(e, inv.id, inv.invoiceNumber)}
                                                                >
                                                                    PDF
                                                                </button>
                                                            </div>
                                                        </td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </section>
                        </div>
                    )}

                    {/* Payments */}
                    {page === "payments" && (
                        <Payments
                            onSelectInvoice={(id) => {
                                setSelectedInvoiceId(id);
                                setPage("details");
                            }}
                        />
                    )}

                    {/* Credit & Debit Notes */}
                    {page === "notes" && <Notes />}

                    {/* Purchases (ITC) */}
                    {page === "purchases" && <Purchases />}

                    {/* Suppliers / Vendors */}
                    {page === "suppliers" && <Suppliers />}

                    {/* Products / Services */}
                    {page === "products" && <ManageData kind="products" />}

                    {/* Customers & Ledger */}
                    {page === "customers" && <ManageData kind="customers" />}

                    {/* Inventory & Stock Movement */}
                    {page === "inventory" && <Inventory />}

                    {/* Reports & Compliance */}
                    {page === "reports" && <Reports />}

                    {/* Audit Logs */}
                    {page === "audit" && <AuditLogs />}

                    {/* Business & Team Settings */}
                    {page === "settings" && <BusinessSettings />}
                </main>
            </div>
        </div>
    );
}

export default App;