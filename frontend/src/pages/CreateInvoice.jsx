import { useEffect, useState } from "react";
import {
    getBusinesses,
    getCustomers,
    getProducts,
    createInvoice
} from "../services/api";

function CreateInvoice({ onBack }) {

    const [businesses, setBusinesses] = useState([]);
    const [customers, setCustomers] = useState([]);
    const [products, setProducts] = useState([]);

    const [businessId, setBusinessId] = useState("");
    const [customerId, setCustomerId] = useState("");

    const [items, setItems] = useState([
        {
            productId: "",
            quantity: 1
        }
    ]);

    const [message, setMessage] = useState("");

    useEffect(() => {

        async function loadData() {
            try {

                const [
                    businessData,
                    customerData,
                    productData
                ] = await Promise.all([
                    getBusinesses(),
                    getCustomers(),
                    getProducts()
                ]);

                setBusinesses(businessData);
                setCustomers(customerData);
                setProducts(productData);

            } catch (error) {

                console.error(error);
                setMessage("Failed to load data");
            }
        }

        loadData();

    }, []);

    function updateItem(index, field, value) {

        const updatedItems = [...items];

        updatedItems[index] = {
            ...updatedItems[index],
            [field]: value
        };

        setItems(updatedItems);
    }

    function addItem() {

        setItems([
            ...items,
            {
                productId: "",
                quantity: 1
            }
        ]);
    }

    function removeItem(index) {

        const updatedItems = items.filter((_, i) => i !== index);

        setItems(updatedItems);
    }

    /*
     * Calculate invoice preview
     */
    function calculateSummary() {

        let taxableAmount = 0;
        let totalTax = 0;

        items.forEach((item) => {

            const product = products.find(
                (product) =>
                    product.id === Number(item.productId)
            );

            if (!product) {
                return;
            }

            const quantity = Number(item.quantity);

            const itemTaxableAmount =
                product.price * quantity;

            const itemTax =
                itemTaxableAmount *
                (product.gstRate / 100);

            taxableAmount += itemTaxableAmount;
            totalTax += itemTax;
        });

        const selectedBusiness = businesses.find(
            (business) =>
                business.id === Number(businessId)
        );

        const selectedCustomer = customers.find(
            (customer) =>
                customer.id === Number(customerId)
        );

        let cgst = 0;
        let sgst = 0;
        let igst = 0;

        if (
            selectedBusiness &&
            selectedCustomer
        ) {

            const intraState =
                selectedBusiness.state?.toLowerCase() ===
                selectedCustomer.state?.toLowerCase();

            if (intraState) {

                cgst = totalTax / 2;
                sgst = totalTax / 2;

            } else {

                igst = totalTax;
            }
        }

        const grandTotal =
            taxableAmount + totalTax;

        return {
            taxableAmount,
            totalTax,
            cgst,
            sgst,
            igst,
            grandTotal
        };
    }

    const summary = calculateSummary();

    async function handleCreateInvoice() {

        if (!businessId || !customerId) {

            setMessage(
                "Please select business and customer"
            );

            return;
        }

        if (
            items.some(
                (item) =>
                    !item.productId ||
                    Number(item.quantity) <= 0
            )
        ) {

            setMessage(
                "Please select a product and valid quantity"
            );

            return;
        }

        const invoice = {

            businessId: Number(businessId),

            customerId: Number(customerId),

            items: items.map((item) => ({
                productId: Number(item.productId),
                quantity: Number(item.quantity)
            }))
        };

        try {

            const createdInvoice =
                await createInvoice(invoice);

            setMessage(
                `Invoice ${createdInvoice.invoiceNumber} created successfully`
            );

        } catch (error) {

            console.error(error);

            setMessage(
                "Failed to create invoice"
            );
        }
    }

    function formatCurrency(amount) {

        return `₹${amount.toLocaleString("en-IN", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        })}`;
    }

    return (
        <div style={styles.container}>

            <button
                onClick={onBack}
                style={styles.backButton}
            >
                ← Back
            </button>

            <h1>Create Invoice</h1>

            {/* Business */}

            <label>Business</label>

            <select
                value={businessId}
                onChange={(e) =>
                    setBusinessId(e.target.value)
                }
                style={styles.input}
            >

                <option value="">
                    Select Business
                </option>

                {businesses.map((business) => (

                    <option
                        key={business.id}
                        value={business.id}
                    >
                        {business.name}
                    </option>

                ))}

            </select>

            {/* Customer */}

            <label>Customer</label>

            <select
                value={customerId}
                onChange={(e) =>
                    setCustomerId(e.target.value)
                }
                style={styles.input}
            >

                <option value="">
                    Select Customer
                </option>

                {customers.map((customer) => (

                    <option
                        key={customer.id}
                        value={customer.id}
                    >
                        {customer.name}
                    </option>

                ))}

            </select>

            <h2>Invoice Items</h2>

            {/* Items */}

            {items.map((item, index) => (

                <div
                    key={index}
                    style={styles.itemRow}
                >

                    <select
                        value={item.productId}
                        onChange={(e) =>
                            updateItem(
                                index,
                                "productId",
                                e.target.value
                            )
                        }
                        style={styles.productSelect}
                    >

                        <option value="">
                            Select Product
                        </option>

                        {products.map((product) => (

                            <option
                                key={product.id}
                                value={product.id}
                            >
                                {product.name} - ₹
                                {product.price}
                            </option>

                        ))}

                    </select>

                    <input
                        type="number"
                        min="1"
                        value={item.quantity}
                        onChange={(e) =>
                            updateItem(
                                index,
                                "quantity",
                                e.target.value
                            )
                        }
                        style={styles.quantityInput}
                    />

                    {items.length > 1 && (

                        <button
                            onClick={() =>
                                removeItem(index)
                            }
                            style={styles.removeButton}
                        >
                            Remove
                        </button>

                    )}

                </div>

            ))}

            <button
                onClick={addItem}
                style={styles.secondaryButton}
            >
                + Add Product
            </button>

            {/* Invoice Summary */}

            {summary.taxableAmount > 0 && (

                <div style={styles.summary}>

                    <h2>Invoice Summary</h2>

                    {items.map((item, index) => {

                        const product = products.find(
                            (product) =>
                                product.id ===
                                Number(item.productId)
                        );

                        if (!product) {
                            return null;
                        }

                        const quantity =
                            Number(item.quantity);

                        const amount =
                            product.price * quantity;

                        return (
                            <div
                                key={index}
                                style={styles.summaryRow}
                            >

                                <span>
                                    {product.name}
                                    {" "}
                                    ({quantity} ×{" "}
                                    {formatCurrency(
                                        product.price
                                    )})
                                </span>

                                <span>
                                    {formatCurrency(amount)}
                                </span>

                            </div>
                        );

                    })}

                    <hr />

                    <div style={styles.summaryRow}>
                        <strong>
                            Taxable Amount
                        </strong>

                        <strong>
                            {formatCurrency(
                                summary.taxableAmount
                            )}
                        </strong>
                    </div>

                    <div style={styles.summaryRow}>
                        <span>CGST</span>

                        <span>
                            {formatCurrency(
                                summary.cgst
                            )}
                        </span>
                    </div>

                    <div style={styles.summaryRow}>
                        <span>SGST</span>

                        <span>
                            {formatCurrency(
                                summary.sgst
                            )}
                        </span>
                    </div>

                    <div style={styles.summaryRow}>
                        <span>IGST</span>

                        <span>
                            {formatCurrency(
                                summary.igst
                            )}
                        </span>
                    </div>

                    <hr />

                    <div style={styles.grandTotal}>
                        <strong>
                            Grand Total
                        </strong>

                        <strong>
                            {formatCurrency(
                                summary.grandTotal
                            )}
                        </strong>
                    </div>

                </div>

            )}

            {/* Create Invoice */}

            <button
                onClick={handleCreateInvoice}
                style={styles.createButton}
            >
                Create Invoice
            </button>

            {message && (

                <p style={styles.message}>
                    {message}
                </p>

            )}

        </div>
    );
}

const styles = {

    container: {
        padding: "40px",
        maxWidth: "900px",
        margin: "0 auto",
        fontFamily: "Arial"
    },

    backButton: {
        padding: "8px 14px",
        cursor: "pointer",
        marginBottom: "20px"
    },

    input: {
        width: "100%",
        padding: "12px",
        fontSize: "16px",
        marginBottom: "20px"
    },

    itemRow: {
        display: "flex",
        gap: "10px",
        alignItems: "center",
        marginBottom: "10px"
    },

    productSelect: {
        flex: 1,
        padding: "12px",
        fontSize: "16px"
    },

    quantityInput: {
        width: "100px",
        padding: "12px",
        fontSize: "16px"
    },

    secondaryButton: {
        padding: "10px 16px",
        cursor: "pointer",
        marginTop: "10px"
    },

    removeButton: {
        padding: "10px",
        cursor: "pointer"
    },

    summary: {
        marginTop: "30px",
        padding: "20px",
        border: "1px solid #ddd",
        borderRadius: "8px"
    },

    summaryRow: {
        display: "flex",
        justifyContent: "space-between",
        padding: "8px 0",
        fontSize: "16px"
    },

    grandTotal: {
        display: "flex",
        justifyContent: "space-between",
        padding: "12px 0",
        fontSize: "20px"
    },

    createButton: {
        width: "100%",
        padding: "14px 20px",
        fontSize: "16px",
        cursor: "pointer",
        marginTop: "25px"
    },

    message: {
        fontSize: "16px",
        marginTop: "15px"
    }
};

export default CreateInvoice;