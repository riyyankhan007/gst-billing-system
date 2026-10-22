import { useEffect, useState } from "react";
import { getInvoices, getProfile, downloadInvoicePdf, pingServer } from "./services/api";

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
import BusinessSwitcher from "./components/BusinessSwitcher";

import Icon from "./components/Icon";
import { SkeletonTable } from "./components/ui/Skeleton";
import EmptyState from "./components/ui/EmptyState";
import ErrorState from "./components/ui/ErrorState";
import ErrorBoundary from "./components/ui/ErrorBoundary";
import { useToast } from "./context/ToastContext";
import "./styles/app.css";

// Hash-based route parser for native browser history (Back / Forward buttons)
function parseRouteFromHash(hashString) {
    const raw = (hashString || (typeof window !== "undefined" ? window.location.hash : "") || "").replace(/^#\/?/, "");
    if (!raw) return { page: "dashboard", invoiceId: null, customerId: null, inventoryTab: "stock" };

    const [pathPart, queryPart] = raw.split("?");
    const parts = pathPart.split("/").filter(Boolean);
    const primary = parts[0] || "dashboard";
    const subId = parts[1] || null;

    const queryParams = new URLSearchParams(queryPart || "");
    const qId = queryParams.get("id");

    if (primary === "details" || primary === "invoice" || (primary === "invoices" && subId)) {
        return { page: "details", invoiceId: subId || qId, customerId: null, inventoryTab: "stock" };
    }
    if (primary === "customer-details" || primary === "customer" || (primary === "customers" && subId)) {
        return { page: "customer-details", customerId: subId || qId, invoiceId: null, inventoryTab: "stock" };
    }

    if (primary === "inventory" || primary === "stock-audit" || primary === "stock-audit-trail" || primary === "audit-trail") {
        const tabParam = queryParams.get("tab") || subId;
        const normalizedTab = (tabParam === "audit" || tabParam === "movements" || tabParam === "trail")
            ? "movements"
            : (tabParam === "warehouses" || tabParam === "facilities" ? "warehouses" : (tabParam === "stock" ? "stock" : null));
        return {
            page: "inventory",
            inventoryTab: normalizedTab || (primary === "stock-audit" || primary === "stock-audit-trail" ? "movements" : "stock"),
            invoiceId: null,
            customerId: null
        };
    }

    return {
        page: primary,
        invoiceId: qId || null,
        customerId: qId || null,
        inventoryTab: "stock"
    };
}

function App() {
    const toast = useToast();
    const [authenticated, setAuthenticated] = useState(
        Boolean(localStorage.getItem("gstToken"))
    );

    const initialRoute = parseRouteFromHash(typeof window !== "undefined" ? window.location.hash : "");
    const [profile, setProfile] = useState(null);
    const [page, setPage] = useState(initialRoute.page);
    const [inventoryTab, setInventoryTab] = useState(initialRoute.inventoryTab || "stock");
    const [invoices, setInvoices] = useState([]);
    const [selectedInvoiceId, setSelectedInvoiceId] = useState(initialRoute.invoiceId);
    const [selectedCustomerId, setSelectedCustomerId] = useState(initialRoute.customerId);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [downloadingPdfId, setDownloadingPdfId] = useState(null);
    const [isServerSlow, setIsServerSlow] = useState(false);
    const [error, setError] = useState("");
    const [searchQuery, setSearchQuery] = useState("");
    const [statusFilter, setStatusFilter] = useState("ALL");
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [authView, setAuthView] = useState("landing"); // "landing" | "login" | "register"
    const [businessVersion, setBusinessVersion] = useState(0);

    // Listen to slow API / cold start state
    useEffect(() => {
        const handleSlow = (e) => {
            setIsServerSlow(Boolean(e.detail?.isSlow));
        };
        window.addEventListener("api-slow-state", handleSlow);
        return () => window.removeEventListener("api-slow-state", handleSlow);
    }, []);


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

    // Prevent background scrolling when mobile navigation drawer is open
    useEffect(() => {
        if (sidebarOpen) {
            const originalOverflow = document.body.style.overflow;
            document.body.style.overflow = "hidden";
            return () => {
                document.body.style.overflow = originalOverflow;
            };
        }
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

    // Keep-alive ping every 60 seconds to prevent Render free-tier backend from sleeping
    useEffect(() => {
        // Initial ping on load
        pingServer();

        // Recurring ping every 60 seconds
        const intervalId = setInterval(() => {
            pingServer();
        }, 60 * 1000);

        // Ping when user returns to this browser tab
        const handleVisibilityChange = () => {
            if (document.visibilityState === "visible") {
                pingServer();
            }
        };

        document.addEventListener("visibilitychange", handleVisibilityChange);

        return () => {
            clearInterval(intervalId);
            document.removeEventListener("visibilitychange", handleVisibilityChange);
        };
    }, []);

    const loadProfile = async () => {
        try {
            const data = await getProfile();
            setProfile(data);
        } catch {
            // Profile load failed, token might be expired
        }
    };

    const loadInvoices = async (preserveExisting = false) => {
        try {
            if (!preserveExisting || invoices.length === 0) {
                setLoading(true);
            } else {
                setRefreshing(true);
            }
            const data = await getInvoices();
            setInvoices(data || []);
            setError("");
        } catch (err) {
            setError(err.message || "Failed to load invoices");
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    };

    useEffect(() => {
        if (authenticated) {
            loadProfile();
            loadInvoices();
        }
    }, [authenticated]);

    // Synchronize browser history (Back / Forward buttons) with application state
    useEffect(() => {
        if (!authenticated) return;

        const handleHashChange = () => {
            const route = parseRouteFromHash(window.location.hash);
            setPage(route.page);
            if (route.invoiceId !== null) setSelectedInvoiceId(route.invoiceId);
            if (route.customerId !== null) setSelectedCustomerId(route.customerId);
            if (route.inventoryTab) setInventoryTab(route.inventoryTab);
            window.scrollTo({ top: 0, behavior: "smooth" });
        };

        if (window.location.hash) {
            handleHashChange();
        } else {
            window.location.hash = "#/dashboard";
        }

        window.addEventListener("hashchange", handleHashChange);
        return () => window.removeEventListener("hashchange", handleHashChange);
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
        window.location.hash = "";
    };

    const navigate = (target, id = null) => {
        let newPath = `/${target}`;
        if ((target === "details" || target === "invoices") && id) {
            newPath = `/invoices/${id}`;
        } else if ((target === "customer-details" || target === "customers") && id) {
            newPath = `/customers/${id}`;
        }

        const targetHash = `#${newPath}`;
        if (window.location.hash !== targetHash) {
            window.location.hash = targetHash;
        } else {
            const route = parseRouteFromHash(targetHash);
            setPage(route.page);
            if (route.invoiceId !== null) setSelectedInvoiceId(route.invoiceId);
            if (route.customerId !== null) setSelectedCustomerId(route.customerId);
        }
        setSidebarOpen(false);
        window.scrollTo({ top: 0, behavior: "smooth" });
    };

    const goBack = (fallback = "dashboard") => {
        if (window.history.length > 1) {
            window.history.back();
        } else {
            navigate(fallback);
        }
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
        if (downloadingPdfId) return;

        try {
            setDownloadingPdfId(id);
            const blob = await downloadInvoicePdf(id);
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = `Invoice-${invoiceNum || id}.pdf`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
            toast.success(`Invoice #${invoiceNum || id} PDF downloaded.`);
        } catch (err) {
            toast.error("Could not download PDF: " + err.message);
        } finally {
            setDownloadingPdfId(null);
        }
    };


    const handleQuickRemind = (e, inv) => {
        e.stopPropagation();
        navigate("details", inv.id);
    };

    const handleBusinessSwitched = async (newAuthData) => {
        // Reset selections and search filters to avoid stale cross-tenant filter leakage
        setSelectedInvoiceId(null);
        setSelectedCustomerId(null);
        setSearchQuery("");
        setStatusFilter("ALL");

        // Concurrently fetch fresh profile and invoices
        await Promise.all([
            loadProfile(),
            loadInvoices()
        ]);

        // Increment version to guarantee clean React remount of all active pages
        setBusinessVersion(prev => prev + 1);

        // Safe navigation if currently viewing an entity belonging to the previous business
        if (page === "details") {
            navigate("invoices");
        } else if (page === "customer-details") {
            navigate("customers");
        } else if (page === "create" || page === "create-invoice") {
            navigate("dashboard");
        }

        window.dispatchEvent(new CustomEvent("business-switched", { detail: newAuthData }));
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

                <div className="mobile-brand" style={{ flex: 1, minWidth: 0, margin: "0 8px" }}>
                    <BusinessSwitcher profile={profile} onBusinessSwitched={handleBusinessSwitched} />
                </div>

                <button
                    type="button"
                    className="action-btn-sm mobile-topbar-action-btn"
                    onClick={() => navigate("create")}
                    title="New Invoice"
                    aria-label="Create New Invoice"
                >
                    <Icon type="plus" size={13} />
                    <span className="mobile-topbar-action-label">Invoice</span>
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
                        <BusinessSwitcher profile={profile} onBusinessSwitched={handleBusinessSwitched} />
                        {isServerSlow && (
                            <span
                                className="refresh-indicator"
                                style={{
                                    backgroundColor: "#fffbeb",
                                    color: "#92400e",
                                    borderColor: "#fde68a",
                                    marginLeft: "8px"
                                }}
                                role="status"
                            >
                                <span
                                    className="inline-spinner"
                                    style={{
                                        width: "10px",
                                        height: "10px",
                                        border: "2px solid #f59e0b",
                                        borderTopColor: "transparent"
                                    }}
                                />
                                <span>Waking up server...</span>
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
                <main className="main-content" key={`biz-${profile?.businessId || "0"}-${businessVersion}`}>
                    {/* Dashboard */}
                    {page === "dashboard" && (
                        <Dashboard
                            onNavigate={navigate}
                            onSelectInvoice={(id) => navigate("details", id)}
                        />
                    )}

                    {/* Create Invoice */}
                    {(page === "create" || page === "create-invoice") && (
                        <CreateInvoice
                            onBack={() => {
                                loadInvoices();
                                goBack("invoices");
                            }}
                            onCreated={(id) => {
                                loadInvoices();
                                navigate("details", id);
                            }}
                            onNavigate={navigate}
                        />
                    )}

                    {/* Invoice Details */}
                    {page === "details" && (
                        <InvoiceDetails
                            invoiceId={selectedInvoiceId}
                            onBack={() => {
                                loadInvoices();
                                goBack("invoices");
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

                                {loading && invoices.length === 0 ? (
                                    <div className="table-card" style={{ padding: 0 }}>
                                        <SkeletonTable
                                            columns={8}
                                            rows={6}
                                            columnWidths={["16%", "20%", "10%", "10%", "14%", "14%", "8%", "8%"]}
                                        />
                                    </div>
                                ) : error && invoices.length === 0 ? (
                                    <ErrorState
                                        title="Failed to load invoices"
                                        message={error}
                                        onRetry={() => loadInvoices()}
                                    />
                                ) : invoices.length === 0 ? (
                                    <EmptyState
                                        icon="invoice"
                                        title="No invoices generated yet"
                                        description="Create your first GST compliant invoice to begin tracking payments and receivables."
                                        actionLabel="Create First Invoice"
                                        onAction={() => navigate("create")}
                                    />
                                ) : filteredInvoices.length === 0 ? (
                                    <EmptyState
                                        isFilterResult={true}
                                        title="No matching invoices found"
                                        description="Try clearing your search query or choosing another status filter."
                                        onClearFilters={() => {
                                            setSearchQuery("");
                                            setStatusFilter("ALL");
                                        }}
                                    />
                                ) : (
                                    <div className="table-responsive" style={{ position: "relative" }}>
                                        {refreshing && (
                                            <div style={{ position: "absolute", top: 12, right: 16, zIndex: 10 }}>
                                                <span className="refresh-indicator">
                                                    <span
                                                        className="inline-spinner"
                                                        style={{
                                                            width: "10px",
                                                            height: "10px",
                                                            border: "2px solid #2563eb",
                                                            borderTopColor: "transparent"
                                                        }}
                                                    />
                                                    Updating...
                                                </span>
                                            </div>
                                        )}
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
                                                            navigate("details", inv.id);
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
                                                                        navigate("customer-details", inv.customer.id);
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
                                                                    className={`action-btn-sm ${downloadingPdfId === inv.id ? "btn-loading" : ""}`}
                                                                    title="Download PDF"
                                                                    disabled={downloadingPdfId === inv.id}
                                                                    onClick={(e) => handleQuickPdfDownload(e, inv.id, inv.invoiceNumber)}
                                                                    style={{ minWidth: "54px", display: "inline-flex", alignItems: "center", justifyContent: "center", gap: "4px" }}
                                                                >
                                                                    {downloadingPdfId === inv.id ? (
                                                                        <>
                                                                            <span
                                                                                className="inline-spinner"
                                                                                style={{
                                                                                    width: "11px",
                                                                                    height: "11px",
                                                                                    border: "2px solid var(--text-muted)",
                                                                                    borderTopColor: "var(--primary)",
                                                                                    borderRadius: "50%"
                                                                                }}
                                                                            />
                                                                            <span style={{ fontSize: "11px" }}>PDF...</span>
                                                                        </>
                                                                    ) : (
                                                                        "PDF"
                                                                    )}
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
                            onSelectInvoice={(id) => navigate("details", id)}
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
                            onBack={() => goBack("customers")}
                            onSelectInvoice={(id) => navigate("details", id)}
                            onNewInvoice={() => navigate("create")}
                        />
                    )}

                    {/* Customers & Ledger */}
                    {page === "customers" && (
                        <ManageData
                            kind="customers"
                            onViewCustomer={(id) => navigate("customer-details", id)}
                        />
                    )}

                    {/* Inventory & Stock Movement */}
                    {page === "inventory" && (
                        <ErrorBoundary title="Inventory Error">
                            <Inventory
                                key={`inventory-${businessVersion}`}
                                defaultTab={inventoryTab}
                                onTabChange={(tab) => setInventoryTab(tab)}
                            />
                        </ErrorBoundary>
                    )}

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