const API_BASE_URL = "http://localhost:8081/api";

export async function getInvoices() {
    const response = await fetch(`${API_BASE_URL}/invoices`);

    if (!response.ok) {
        throw new Error("Failed to fetch invoices");
    }

    return response.json();
}

export async function getBusinesses() {
    const response = await fetch(`${API_BASE_URL}/business`);

    if (!response.ok) {
        throw new Error("Failed to fetch businesses");
    }

    return response.json();
}

export async function getCustomers() {
    const response = await fetch(`${API_BASE_URL}/customers`);

    if (!response.ok) {
        throw new Error("Failed to fetch customers");
    }

    return response.json();
}

export async function getProducts() {
    const response = await fetch(`${API_BASE_URL}/products`);

    if (!response.ok) {
        throw new Error("Failed to fetch products");
    }

    return response.json();
}

export async function createInvoice(invoice) {
    const response = await fetch(`${API_BASE_URL}/invoices`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(invoice)
    });

    if (!response.ok) {
        throw new Error("Failed to create invoice");
    }

    return response.json();
}

export async function getInvoiceById(id) {
    const response = await fetch(
        `${API_BASE_URL}/invoices/${id}`
    );

    if (!response.ok) {
        throw new Error("Failed to fetch invoice");
    }

    return response.json();
}

export async function markInvoiceAsPaid(id) {
    const response = await fetch(
        `${API_BASE_URL}/invoices/${id}/paid`,
        {
            method: "PUT"
        }
    );

    if (!response.ok) {
        throw new Error("Failed to mark invoice as paid");
    }

    return response.json();
}

export async function cancelInvoice(id) {
    const response = await fetch(
        `${API_BASE_URL}/invoices/${id}/cancel`,
        {
            method: "PUT"
        }
    );

    if (!response.ok) {
        throw new Error("Failed to cancel invoice");
    }

    return response.json();
}