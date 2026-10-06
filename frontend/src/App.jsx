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
import CustomerDetails from "./pages/CustomerDetails";

import Icon from "./components/Icon";
import "./styles/app.css";

function App() {
    const [authenticated, setAuthenticated] = useState(
        Boolean(localStorage.getItem("gstToken"))
    );

    const [profile, setProfile] = useState(null);
    const [page, setPage] = useState("dashboard");
    const [invoices, setInvoices] = useState([]);
    const [selectedInvoiceId, setSelectedInvoiceId] = useState(null);
    const [selectedCustomerId, setSelectedCustomerId] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [searchQuery, setSearchQuery] = useState("");
    const [statusFilter, setStatusFilter] = useState("ALL");
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [authView, setAuthView] = useState("landing"); // "landing" | "login" | "register"

    // Close sidebar on Escape key
    useEffect(() => {
        const handleKeyDown = (e) => {
            if (e.key === "Escape" && sidebarOpen) {
                setSidebarOpen(false);
            }
        };
        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [sidebarOpen]);

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
            title={label}
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
                    <span>{profile?.businessName || "GST Billing System"}</span>
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

            {/* Backdrop for sidebar drawer */}
            <div
                className={`sidebar-overlay ${sidebarOpen ? "active" : ""}`}
                onClick={() => setSidebarOpen(false)}
            />

            {/* Sidebar Drawer */}
            <aside className={`sidebar ${sidebarOpen ? "open" : ""}`}>
                <div className="sidebar-brand">
                    <button
                        type="button"
                        className="sidebar-brand-toggle-btn"
                        onClick={() => setSidebarOpen(false)}
                        title="Close Menu"
                        aria-label="Close Navigation Menu"
                    >
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <line x1="3" y1="12" x2="21" y2="12" />
                            <line x1="3" y1="6" x2="21" y2="6" />
                            <line x1="3" y1="18" x2="21" y2="18" />
                        </svg>
                    </button>
                    <div className="sidebar-brand-text">
                        <strong>{profile?.businessName || "GST Billing"}</strong>
                        <span>Multi-Tenant SaaS ERP</span>
                    </div>
                    <button
                        type="button"
                        className="sidebar-close-btn"
                        onClick={() => setSidebarOpen(false)}
                        aria-label="Close sidebar"
                        title="Close"
                    >
                        <Icon type="close" size={15} />
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
                                {profile.name ? profile.name.trim().charAt(0).toUpperCase() : "U"}
                            </div>
                            <div className="sidebar-user-info">
                                <div className="sidebar-user-name">
                                    {profile.name || "User"}
                                </div>
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
                        <button
                            type="button"
                            className="sidebar-toggle-btn"
                            onClick={() => setSidebarOpen(prev => !prev)}
                            title={sidebarOpen ? "Close Menu" : "Open Menu"}
                            aria-label="Toggle Navigation Menu"
                        >
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <line x1="3" y1="12" x2="21" y2="12" />
                                <line x1="3" y1="6" x2="21" y2="6" />
                                <line x1="3" y1="18" x2="21" y2="18" />
                            </svg>
                        </button>
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
                                            {searchQuery && (
                                                <button
                                                    type="button"
                                                    className="search-clear-btn"
                                                    onClick={() => setSearchQuery("")}
                                                    title="Clear search"
                                                >
                                                    <Icon type="close" size={13} />
                                                </button>
                                            )}
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
                                                            <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                                                                <span style={{ fontWeight: 600 }}>{inv.invoiceNumber}</span>
                                                                {inv.irn && (
                                                                    <span
                                                                        className="badge"
                                                                        style={{
                                                                            backgroundColor: "#ecfdf5",
                                                                            color: "#065f46",
                                                                            fontSize: "10px",
                                                                            fontWeight: "700",
                                                                            padding: "2px 6px",
                                                                            borderRadius: "4px",
                                                                            border: "1px solid #a7f3d0"
                                                                        }}
                                                                        title={`E-Invoice Generated (IRN: ${inv.irn.substring(0, 10)}...)`}
                                                                    >
                                                                        IRN
                                                                    </span>
                                                                )}
                                                                {(inv.hasEwayBill || (inv.ewayBills && inv.ewayBills.length > 0)) && (
                                                                    <span
                                                                        className="badge"
                                                                        style={{
                                                                            backgroundColor: "#fffbeb",
                                                                            color: "#92400e",
                                                                            fontSize: "10px",
                                                                            fontWeight: "700",
                                                                            padding: "2px 6px",
                                                                            borderRadius: "4px",
                                                                            border: "1px solid #fde68a"
                                                                        }}
                                                                        title="E-Way Bill Generated"
                                                                    >
                                                                        EWB
                                                                    </span>
                                                                )}
                                                            </div>
                                                        </td>
                                                        <td>
                                                            {inv.customer?.id ? (
                                                                <button
                                                                    type="button"
                                                                    onClick={(e) => {
                                                                        e.stopPropagation();
                                                                        setSelectedCustomerId(inv.customer.id);
                                                                        setPage("customer-details");
                                                                        window.scrollTo({ top: 0, behavior: "smooth" });
                                                                    }}
                                                                    style={{
                                                                        background: "none",
                                                                        border: "none",
                                                                        padding: 0,
                                                                        textAlign: "left",
                                                                        fontWeight: "700",
                                                                        color: "var(--primary)",
                                                                        cursor: "pointer",
                                                                        fontSize: "13.5px"
                                                                    }}
                                                                    title="View customer profile & ledger"
                                                                >
                                                                    {inv.customer.name}
                                                                </button>
                                                            ) : (
                                                                <strong>{inv.customer?.name || "—"}</strong>
                                                            )}
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

                    {/* Customer 360 Profile & Details */}
                    {page === "customer-details" && (
                        <CustomerDetails
                            customerId={selectedCustomerId}
                            onBack={() => setPage("customers")}
                            onSelectInvoice={(id) => {
                                setSelectedInvoiceId(id);
                                setPage("details");
                            }}
                            onNewInvoice={() => {
                                setPage("create");
                            }}
                        />
                    )}

                    {/* Customers & Ledger */}
                    {page === "customers" && (
                        <ManageData
                            kind="customers"
                            onViewCustomer={(id) => {
                                setSelectedCustomerId(id);
                                setPage("customer-details");
                                window.scrollTo({ top: 0, behavior: "smooth" });
                            }}
                        />
                    )}

                    {/* Inventory & Stock Movement */}
                    {page === "inventory" && <Inventory />}

                    {/* Reports & Compliance */}
                    {page === "reports" && <Reports />}

                    {/* Audit Logs */}
                    {page === "audit" && <AuditLogs />}

                    {/* Business & Team Settings */}
                    {page === "settings" && <BusinessSettings />}
                </main>

                {/* Universal App Footer */}
                <footer className="app-universal-footer">
                    <div className="footer-content">
                        <div className="footer-left">
                            <span className="footer-status-indicator">
                                <span className="status-dot-pulse" />
                                System Operational
                            </span>
                            <span className="footer-separator">•</span>
                            <span>GSTN E-Way & E-Invoice Compliant</span>
                            <span className="footer-separator">•</span>
                            <span>© {new Date().getFullYear()} {profile?.businessName || "GST Billing ERP"}</span>
                        </div>
                        <div className="footer-right">
                            <span className="footer-pill">Security: 256-Bit TLS</span>
                            <span className="footer-pill">Role: {profile?.role || "USER"}</span>
                            <span className="footer-version">v2.4.0</span>
                        </div>
                    </div>
                </footer>
            </div>
        </div>
    );
}

export default App;