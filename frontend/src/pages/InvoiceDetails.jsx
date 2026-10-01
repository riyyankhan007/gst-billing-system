import { useEffect, useState } from "react";
import {
    getInvoiceById,
    markInvoiceAsPaid,
    cancelInvoice
} from "../services/api";

function InvoiceDetails({ invoiceId, onBack }) {
    const [invoice, setInvoice] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        async function loadInvoice() {
            try {
                const data = await getInvoiceById(invoiceId);
                setInvoice(data);
            } catch (error) {
                console.error(error);
                setError("Failed to load invoice");
            } finally {
                setLoading(false);
            }
        }

        loadInvoice();
    }, [invoiceId]);

    function formatCurrency(amount) {
        return `₹${Number(amount).toLocaleString("en-IN", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        })}`;
    }

    function downloadPdf() {
        window.open(
            `http://localhost:8081/api/invoices/${invoice.id}/pdf`,
            "_blank"
        );
    }

    async function handlePaid() {
        try {
            const updated = await markInvoiceAsPaid(invoice.id);
            setInvoice(updated);
        } catch (error) {
            alert(error.message);
        }
    }

    async function handleCancel() {
        try {
            const updated = await cancelInvoice(invoice.id);
            setInvoice(updated);
        } catch (error) {
            alert(error.message);
        }
    }

    if (loading) {
        return <p style={styles.message}>Loading invoice...</p>;
    }

    if (error) {
        return <p style={styles.error}>{error}</p>;
    }

    if (!invoice) {
        return <p style={styles.error}>Invoice not found</p>;
    }

    return (
        <div style={styles.container}>

            <button onClick={onBack} style={styles.backButton}>
                ← Back
            </button>

            <div style={styles.invoice}>

                <div style={styles.header}>

                    <div style={styles.business}>
                        <h1>{invoice.business?.name}</h1>
                        <p>{invoice.business?.address}</p>
                        <p>GSTIN: {invoice.business?.gstin}</p>
                        <p>State: {invoice.business?.state}</p>
                    </div>

                    <div style={styles.invoiceInfo}>
                        <h2>TAX INVOICE</h2>
                        <p>
                            <strong>Invoice No:</strong>{" "}
                            {invoice.invoiceNumber}
                        </p>
                        <p>
                            <strong>Date:</strong>{" "}
                            {invoice.invoiceDate}
                        </p>
                        <p>
                            <strong>Status:</strong>{" "}
                            {invoice.status}
                        </p>
                    </div>

                </div>

                <hr />

                <div style={styles.billTo}>
                    <h3>Bill To</h3>
                    <p>
                        <strong>{invoice.customer?.name}</strong>
                    </p>
                    <p>{invoice.customer?.address}</p>
                    <p>GSTIN: {invoice.customer?.gstin}</p>
                    <p>State: {invoice.customer?.state}</p>
                </div>

                <hr />

                <h2>Invoice Items</h2>

                <table style={styles.table}>
                    <thead>
                        <tr>
                            <th style={styles.cell}>Product</th>
                            <th style={styles.cell}>HSN</th>
                            <th style={styles.cell}>Qty</th>
                            <th style={styles.cell}>Price</th>
                            <th style={styles.cell}>GST</th>
                            <th style={styles.cell}>Tax</th>
                            <th style={styles.cell}>Total</th>
                        </tr>
                    </thead>

                    <tbody>
                        {invoice.items.map((item) => (
                            <tr key={item.id}>
                                <td style={styles.cell}>
                                    {item.productName}
                                </td>
                                <td style={styles.cell}>
                                    {item.hsnCode}
                                </td>
                                <td style={styles.cell}>
                                    {item.quantity}
                                </td>
                                <td style={styles.cell}>
                                    {formatCurrency(item.unitPrice)}
                                </td>
                                <td style={styles.cell}>
                                    {item.gstRate}%
                                </td>
                                <td style={styles.cell}>
                                    {formatCurrency(item.taxAmount)}
                                </td>
                                <td style={styles.cell}>
                                    {formatCurrency(item.totalAmount)}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>

                <div style={styles.summary}>

                    <div style={styles.summaryRow}>
                        <span>Taxable Amount</span>
                        <strong>
                            {formatCurrency(invoice.taxableAmount)}
                        </strong>
                    </div>

                    <div style={styles.summaryRow}>
                        <span>CGST</span>
                        <span>{formatCurrency(invoice.cgst)}</span>
                    </div>

                    <div style={styles.summaryRow}>
                        <span>SGST</span>
                        <span>{formatCurrency(invoice.sgst)}</span>
                    </div>

                    <div style={styles.summaryRow}>
                        <span>IGST</span>
                        <span>{formatCurrency(invoice.igst)}</span>
                    </div>

                    <hr />

                    <div style={styles.grandTotal}>
                        <strong>Grand Total</strong>
                        <strong>
                            {formatCurrency(invoice.grandTotal)}
                        </strong>
                    </div>

                </div>

                <div style={styles.actions}>

                    {invoice.status === "DRAFT" && (
                        <>
                            <button
                                onClick={handlePaid}
                                style={styles.paidButton}
                            >
                                Mark as Paid
                            </button>

                            <button
                                onClick={handleCancel}
                                style={styles.cancelButton}
                            >
                                Cancel Invoice
                            </button>
                        </>
                    )}

                    <button
                        onClick={downloadPdf}
                        style={styles.pdfButton}
                    >
                        Download PDF
                    </button>

                </div>

            </div>
        </div>
    );
}

const styles = {
    container: {
        padding: "40px",
        maxWidth: "1100px",
        margin: "0 auto",
        fontFamily: "Arial"
    },

    backButton: {
        padding: "10px 16px",
        cursor: "pointer",
        marginBottom: "20px"
    },

    invoice: {
        border: "1px solid #ddd",
        padding: "30px",
        borderRadius: "8px"
    },

    header: {
        display: "flex",
        justifyContent: "space-between",
        gap: "40px"
    },

    business: {
        flex: 1
    },

    invoiceInfo: {
        textAlign: "right"
    },

    billTo: {
        marginTop: "20px",
        marginBottom: "20px"
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

    summary: {
        marginTop: "30px",
        marginLeft: "auto",
        maxWidth: "400px"
    },

    summaryRow: {
        display: "flex",
        justifyContent: "space-between",
        padding: "8px 0"
    },

    grandTotal: {
        display: "flex",
        justifyContent: "space-between",
        fontSize: "22px",
        padding: "12px 0"
    },

    actions: {
        marginTop: "25px",
        display: "flex",
        gap: "10px",
        justifyContent: "flex-end"
    },

    paidButton: {
        padding: "12px 20px",
        cursor: "pointer"
    },

    cancelButton: {
        padding: "12px 20px",
        cursor: "pointer"
    },

    pdfButton: {
        padding: "12px 20px",
        cursor: "pointer",
        border: "none",
        borderRadius: "6px",
        backgroundColor: "#222",
        color: "white"
    },

    message: {
        padding: "40px",
        textAlign: "center"
    },

    error: {
        padding: "40px",
        textAlign: "center",
        color: "red"
    }
};

export default InvoiceDetails;