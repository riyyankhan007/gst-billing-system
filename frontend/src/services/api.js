const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8081/api";
const token = () => localStorage.getItem("gstToken");

async function request(path, options = {}) {
    const headers = {
        ...(options.body instanceof FormData ? {} : { "Content-Type": "application/json" }),
        ...(options.headers || {})
    };

    if (token()) headers.Authorization = `Bearer ${token()}`;

    const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });

    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || "Request failed");
    }

    return response;
}

const json = (path, options) => request(path, options).then(r => r.json());

export const login = body => json("/auth/login", { method: "POST", body: JSON.stringify(body) });
export const register = body => json("/auth/register", { method: "POST", body: JSON.stringify(body) });

export const getInvoices = () => json("/invoices");
export const getBusiness = () => json("/business");
export const getBusinesses = async () => [await getBusiness()];
export const updateBusiness = body => json("/business", { method: "PUT", body: JSON.stringify(body) });
export const uploadLogo = file => {
    const form = new FormData();
    form.append("file", file);
    return json("/business/logo", { method: "POST", body: form });
};

export const getCustomers = () => json("/customers");
export const createCustomer = body => json("/customers", { method: "POST", body: JSON.stringify(body) });

export const getProducts = () => json("/products");
export const createProduct = body => json("/products", { method: "POST", body: JSON.stringify(body) });

export const createInvoice = body => json("/invoices", { method: "POST", body: JSON.stringify(body) });
export const getInvoiceById = id => json(`/invoices/${id}`);
export const markInvoiceAsPaid = id => json(`/invoices/${id}/paid`, { method: "PUT" });
export const cancelInvoice = id => json(`/invoices/${id}/cancel`, { method: "PUT" });

export async function downloadInvoicePdf(id) {
    return (await request(`/invoices/${id}/pdf`)).blob();
}
