import { useEffect, useState } from "react";
import { getInvoices } from "./services/api";
import CreateInvoice from "./pages/CreateInvoice";
import InvoiceDetails from "./pages/InvoiceDetails";

function App() {

    const [page, setPage] = useState("dashboard");
    const [invoices, setInvoices] = useState([]);
    const [selectedInvoiceId, setSelectedInvoiceId] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    async function loadInvoices() {

        try {

            setLoading(true);

            const data = await getInvoices();

            setInvoices(data);
            setError("");

        } catch (error) {

            console.error(error);
            setError("Failed to load invoices");

        } finally {

            setLoading(false);
        }
    }

    useEffect(() => {
        loadInvoices();
    }, []);

    // Create Invoice page
    if (page === "create") {

        return (
            <CreateInvoice
                onBack={() => {
                    setPage("dashboard");
                    loadInvoices();
                }}
            />
        );
    }

    // Invoice Details page
    if (page === "details") {

        return (
            <InvoiceDetails
                invoiceId={selectedInvoiceId}
                onBack={() => {
                    setPage("dashboard");
                    setSelectedInvoiceId(null);
                    loadInvoices();
                }}
            />
        );
    }

    // Dashboard
    return (
        <div style={styles.container}>

            <h1>GST Billing</h1>

            <p>Dashboard</p>

            <button
                onClick={() => setPage("create")}
                style={styles.createButton}
            >
                + Create Invoice
            </button>

            <h2 style={{ marginTop: "40px" }}>
                Recent Invoices
            </h2>

            {loading && (
                <p>
                    Loading invoices...
                </p>
            )}

            {error && (
                <p style={{ color: "red" }}>
                    {error}
                </p>
            )}

            {!loading && !error && (

                <table style={styles.table}>

                    <thead>

                        <tr>

                            <th style={styles.cell}>
                                Invoice
                            </th>

                            <th style={styles.cell}>
                                Customer
                            </th>

                            <th style={styles.cell}>
                                Amount
                            </th>

                            <th style={styles.cell}>
                                Status
                            </th>

                        </tr>

                    </thead>

                    <tbody>

                        {invoices.map((invoice) => (

                            <tr key={invoice.id}>

                                <td style={styles.cell}>

                                    <button
                                        onClick={() => {
                                            setSelectedInvoiceId(
                                                invoice.id
                                            );
                                            setPage("details");
                                        }}
                                        style={styles.invoiceLink}
                                    >
                                        {invoice.invoiceNumber}
                                    </button>

                                </td>

                                <td style={styles.cell}>
                                    {invoice.customer?.name}
                                </td>

                                <td style={styles.cell}>
                                    ₹
                                    {Number(
                                        invoice.grandTotal
                                    ).toLocaleString("en-IN", {
                                        minimumFractionDigits: 2,
                                        maximumFractionDigits: 2
                                    })}
                                </td>

                                <td style={styles.cell}>
                                    {invoice.status}
                                </td>

                            </tr>

                        ))}

                    </tbody>

                </table>

            )}

        </div>
    );
}

const styles = {

    container: {
        padding: "40px",
        fontFamily: "Arial"
    },

    createButton: {
        padding: "12px 20px",
        fontSize: "16px",
        cursor: "pointer"
    },

    table: {
        width: "100%",
        borderCollapse: "collapse",
        marginTop: "20px"
    },

    cell: {
        border: "1px solid #ddd",
        padding: "12px",
        textAlign: "left"
    },

    invoiceLink: {
        background: "none",
        border: "none",
        padding: 0,
        cursor: "pointer",
        fontSize: "16px"
    }
};

export default App;