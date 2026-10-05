const API_BASE_URL =
    import.meta.env.VITE_API_URL || "http://localhost:8081/api";

const token = () => localStorage.getItem("gstToken");

async function request(path, options = {}) {
    const headers = {
        ...(options.body instanceof FormData
            ? {}
            : { "Content-Type": "application/json" }),
        ...(options.headers || {})
    };

    // Public auth endpoints don't need token
    const isPublicAuthRequest =
        path === "/auth/login" ||
        path === "/auth/register" ||
        path === "/auth/forgot-password" ||
        path === "/auth/reset-password";

    if (!isPublicAuthRequest && token()) {
        headers.Authorization = `Bearer ${token()}`;
    }

    const response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers
    });

    if (!response.ok) {
        let errorMessage = "Request failed";
        try {
            const errorText = await response.text();
            try {
                const parsed = JSON.parse(errorText);
                errorMessage = parsed.message || parsed.error || errorText;
            } catch {
                errorMessage = errorText || `HTTP ${response.status}: ${response.statusText}`;
            }
        } catch {
            errorMessage = `HTTP ${response.status}`;
        }

        if (response.status === 401 && !isPublicAuthRequest) {
            localStorage.removeItem("gstToken");
            window.dispatchEvent(new Event("auth-expired"));
        }

        throw new Error(errorMessage);
    }

    return response;
}

const json = (path, options) =>
    request(path, options).then(r => r.json());

// =========================
// AUTH
// =========================

export const login = body =>
    json("/auth/login", {
        method: "POST",
        body: JSON.stringify(body)
    });

export const register = body =>
    json("/auth/register", {
        method: "POST",
        body: JSON.stringify(body)
    });

export const forgotPassword = body =>
    json("/auth/forgot-password", {
        method: "POST",
        body: JSON.stringify(body)
    });

export const resetPassword = body =>
    json("/auth/reset-password", {
        method: "POST",
        body: JSON.stringify(body)
    });

export const changePassword = body =>
    json("/auth/change-password", {
        method: "POST",
        body: JSON.stringify(body)
    });

export const getProfile = () =>
    json("/auth/me");

// =========================
// BUSINESS
// =========================

export const getBusiness = () =>
    json("/business");

export const getBusinesses = () =>
    getBusiness().then(b => (b ? [b] : [])).catch(() => []);

export const updateBusiness = body =>
    json("/business", {
        method: "PUT",
        body: JSON.stringify(body)
    });

export const uploadLogo = file => {
    const form = new FormData();
    form.append("file", file);
    return json("/business/logo", {
        method: "POST",
        body: form
    });
};

export const uploadSignature = file => {
    const form = new FormData();
    form.append("file", file);
    return json("/business/signature", {
        method: "POST",
        body: form
    });
};

// =========================
// CUSTOMERS
// =========================

export const getCustomers = (search = "") =>
    json(search ? `/customers?search=${encodeURIComponent(search)}` : "/customers");

export const getCustomerById = id =>
    json(`/customers/${id}`);

export const getCustomerDetails = id =>
    json(`/customers/${id}/details`);

export const createCustomer = body =>
    json("/customers", {
        method: "POST",
        body: JSON.stringify(body)
    });

export const updateCustomer = (id, body) =>
    json(`/customers/${id}`, {
        method: "PUT",
        body: JSON.stringify(body)
    });

export const deleteCustomer = id =>
    request(`/customers/${id}`, {
        method: "DELETE"
    });

// =========================
// PRODUCTS & SERVICES
// =========================

export const getProducts = (search = "") =>
    json(search ? `/products?search=${encodeURIComponent(search)}` : "/products");

export const getProductConfig = () =>
    json("/products/config");

export const getProductById = id =>
    json(`/products/${id}`);

export const createProduct = body =>
    json("/products", {
        method: "POST",
        body: JSON.stringify(body)
    });

export const updateProduct = (id, body) =>
    json(`/products/${id}`, {
        method: "PUT",
        body: JSON.stringify(body)
    });

export const deleteProduct = id =>
    request(`/products/${id}`, {
        method: "DELETE"
    });

// =========================
// INVOICES & REMINDERS
// =========================

export const getInvoices = (status = "", customerId = "") => {
    const params = new URLSearchParams();
    if (status) params.append("status", status);
    if (customerId) params.append("customerId", customerId);
    const qs = params.toString() ? `?${params.toString()}` : "";
    return json(`/invoices${qs}`);
};

export const createInvoice = (body, idempotencyKey = null) =>
    json("/invoices", {
        method: "POST",
        headers: idempotencyKey ? { "X-Idempotency-Key": idempotencyKey } : {},
        body: JSON.stringify(body)
    });

export const getInvoiceById = id =>
    json(`/invoices/${id}`);

export const updateDraftInvoice = (id, body) =>
    json(`/invoices/${id}`, {
        method: "PUT",
        body: JSON.stringify(body)
    });

export const issueInvoice = id =>
    json(`/invoices/${id}/issue`, {
        method: "PUT"
    });

export const markInvoiceAsSent = id =>
    json(`/invoices/${id}/sent`, {
        method: "PUT"
    });

export const cancelInvoice = id =>
    json(`/invoices/${id}/cancel`, {
        method: "PUT"
    });

export const deleteInvoice = id =>
    request(`/invoices/${id}`, {
        method: "DELETE"
    });

export async function downloadInvoicePdf(id) {
    const res = await request(`/invoices/${id}/pdf`);
    return res.blob();
}

export const getInvoiceReminder = id =>
    json(`/invoices/${id}/reminder`);

export const sendInvoiceReminderEmail = (id, note) =>
    json(`/invoices/${id}/remind-email`, {
        method: "POST",
        body: JSON.stringify({ note })
    });

// =========================
// E-INVOICE & E-WAY BILL
// =========================

export const generateEInvoice = id =>
    json(`/invoices/${id}/einvoice/generate`, { method: "POST" });

export const cancelEInvoice = (id, reason, remarks) =>
    json(`/invoices/${id}/einvoice/cancel`, {
        method: "POST",
        body: JSON.stringify({ reason, remarks })
    });

export const getEInvoice = id =>
    json(`/invoices/${id}/einvoice`);

export const generateEWayBill = (id, body) =>
    json(`/invoices/${id}/ewaybill/generate`, {
        method: "POST",
        body: JSON.stringify(body || {})
    });

export const cancelEWayBill = (id, ewbId, cancelReason, cancelRemarks) =>
    json(`/invoices/${id}/ewaybill/${ewbId}/cancel`, {
        method: "POST",
        body: JSON.stringify({ cancelReason, cancelRemarks })
    });

export const getEWayBills = id =>
    json(`/invoices/${id}/ewaybill`);

// =========================
// PAYMENTS
// =========================

export const getPayments = () =>
    json("/payments");

export const getPaymentsForInvoice = invoiceId =>
    json(`/payments/invoice/${invoiceId}`);

export const getPaymentsForCustomer = customerId =>
    json(`/payments/customer/${customerId}`);

export const recordPayment = (body, idempotencyKey = null) =>
    json("/payments", {
        method: "POST",
        headers: idempotencyKey ? { "X-Idempotency-Key": idempotencyKey } : {},
        body: JSON.stringify(body)
    });

export const deletePayment = id =>
    request(`/payments/${id}`, {
        method: "DELETE"
    });

export async function downloadPaymentReceiptPdf(paymentId) {
    const res = await request(`/payments/${paymentId}/receipt`);
    return res.blob();
}

export const reconcileAllPayments = () =>
    json("/payments/reconcile", { method: "POST" });

export const createPaymentGatewayOrder = body =>
    json("/payments/order", {
        method: "POST",
        body: JSON.stringify(body)
    });

// =========================
// SYSTEM HEALTH & METRICS
// =========================

export const getSystemHealth = () =>
    json("/health");

export const getSystemMetrics = () =>
    json("/metrics");

// =========================
// DASHBOARD
// =========================

export const getDashboardMetrics = () =>
    json("/dashboard");

// =========================
// REPORTS & DATA EXPORT
// =========================

export const getSalesReport = (startDate, endDate) => {
    const params = new URLSearchParams();
    if (startDate) params.append("startDate", startDate);
    if (endDate) params.append("endDate", endDate);
    return json(`/reports/sales?${params.toString()}`);
};

export const getGstReport = (startDate, endDate) => {
    const params = new URLSearchParams();
    if (startDate) params.append("startDate", startDate);
    if (endDate) params.append("endDate", endDate);
    return json(`/reports/gst?${params.toString()}`);
};

export const getCustomerReport = () =>
    json("/reports/customers");

export const getProductReport = () =>
    json("/reports/products");

export async function downloadExportCsv(type) {
    const res = await request(`/reports/export/${type}`);
    return res.blob();
}

// =========================
// SUPPLIERS
// =========================

export const getSuppliers = (search = "") =>
    json(search ? `/suppliers?search=${encodeURIComponent(search)}` : "/suppliers");

export const getSupplierById = id =>
    json(`/suppliers/${id}`);

export const getSupplierDetails = id =>
    json(`/suppliers/${id}/details`);

export const createSupplier = body =>
    json("/suppliers", {
        method: "POST",
        body: JSON.stringify(body)
    });

export const updateSupplier = (id, body) =>
    json(`/suppliers/${id}`, {
        method: "PUT",
        body: JSON.stringify(body)
    });

export const deleteSupplier = id =>
    request(`/suppliers/${id}`, {
        method: "DELETE"
    });

// =========================
// PURCHASES
// =========================

export const getPurchases = () =>
    json("/purchases");

export const getPurchaseById = id =>
    json(`/purchases/${id}`);

export const createPurchase = body =>
    json("/purchases", {
        method: "POST",
        body: JSON.stringify(body)
    });

export const recordPurchasePayment = (id, amount) =>
    json(`/purchases/${id}/payment`, {
        method: "POST",
        body: JSON.stringify({ amount })
    });

export const deletePurchase = id =>
    request(`/purchases/${id}`, {
        method: "DELETE"
    });

// =========================
// INVENTORY
// =========================

export const getInventoryMovements = () =>
    json("/inventory/movements");

export const adjustStock = (productId, quantity, reason) =>
    json("/inventory/adjust", {
        method: "POST",
        body: JSON.stringify({ productId, quantity, reason })
    });

// =========================
// CREDIT & DEBIT NOTES
// =========================

export const getCreditNotes = () =>
    json("/credit-notes");

export const getCreditNoteById = id =>
    json(`/credit-notes/${id}`);

export const createCreditNote = body =>
    json("/credit-notes", {
        method: "POST",
        body: JSON.stringify(body)
    });

export const cancelCreditNote = id =>
    json(`/credit-notes/${id}/cancel`, {
        method: "PUT"
    });

export const getDebitNotes = () =>
    json("/debit-notes");

export const getDebitNoteById = id =>
    json(`/debit-notes/${id}`);

export const createDebitNote = body =>
    json("/debit-notes", {
        method: "POST",
        body: JSON.stringify(body)
    });

export const cancelDebitNote = id =>
    json(`/debit-notes/${id}/cancel`, {
        method: "PUT"
    });

// =========================
// USERS & ROLES
// =========================

export const getBusinessUsers = () =>
    json("/users");

export const createBusinessUser = body =>
    json("/users", {
        method: "POST",
        body: JSON.stringify(body)
    });

export const updateUserRole = (id, role) =>
    json(`/users/${id}/role`, {
        method: "PUT",
        body: JSON.stringify({ role })
    });

// =========================
// AUDIT LOGS
// =========================

export const getAuditLogs = () =>
    json("/audit-logs");

// =========================
// DASHBOARD & ANALYTICS
// =========================

export const getAnalytics = (range = "MONTH", startDate = null, endDate = null) => {
    const params = new URLSearchParams();
    if (range) params.append("range", range);
    if (startDate) params.append("startDate", startDate);
    if (endDate) params.append("endDate", endDate);
    const qs = params.toString();
    return json(`/analytics${qs ? `?${qs}` : ""}`);
};