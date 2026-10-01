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
        <div className="invoice-page management-page">
            <button className="text-button" onClick={onBack}>← Back</button>

            <div className="page-intro">
                <div className="eyebrow">INVOICES</div>
                <h1>Create an invoice.</h1>
                <p>Build your invoice, add products and review the total.</p>
            </div>

            <div className="management-layout">
                <section className="form-panel">
                    <div className="form-heading">
                        <div className="eyebrow">DETAILS</div>
                        <h2>Invoice details</h2>
                    </div>

                    <div className="form-field">
                        <label>Business</label>
                        <select value={businessId} onChange={e => setBusinessId(e.target.value)}>
                            <option value="">Select Business</option>
                            {businesses.map(business => (
                                <option key={business.id} value={business.id}>{business.name}</option>
                            ))}
                        </select>
                    </div>

                    <div className="form-field">
                        <label>Customer</label>
                        <select value={customerId} onChange={e => setCustomerId(e.target.value)}>
                            <option value="">Select Customer</option>
                            {customers.map(customer => (
                                <option key={customer.id} value={customer.id}>{customer.name}</option>
                            ))}
                        </select>
                    </div>

                    <div className="form-heading invoice-items-heading">
                        <div className="eyebrow">ITEMS</div>
                        <h2>Invoice items</h2>
                    </div>

                    {items.map((item, index) => (
                        <div className="invoice-item" key={index}>
                            <select
                                value={item.productId}
                                onChange={e => updateItem(index, "productId", e.target.value)}
                            >
                                <option value="">Select Product</option>
                                {products.map(product => (
                                    <option key={product.id} value={product.id}>
                                        {product.name} — ₹{product.price}
                                    </option>
                                ))}
                            </select>

                            <input
                                type="number"
                                min="1"
                                value={item.quantity}
                                onChange={e => updateItem(index, "quantity", e.target.value)}
                            />

                            {items.length > 1 && (
                                <button type="button" onClick={() => removeItem(index)}>×</button>
                            )}
                        </div>
                    ))}

                    <button type="button" className="add-item-button" onClick={addItem}>
                        + Add product
                    </button>

                    <div className="invoice-actions">
                        <button className="primary-button form-submit" onClick={handleCreateInvoice}>
                            Create invoice
                        </button>
                    </div>

                    {message && <p className="form-error">{message}</p>}
                </section>

                <section className="data-panel">
                    <div className="data-panel-header">
                        <div className="eyebrow">SUMMARY</div>
                        <h2>Invoice preview</h2>
                    </div>

                    <div className="data-list">
                        {items.map((item, index) => {
                            const product = products.find(p => p.id === Number(item.productId));
                            if (!product) return null;

                            const amount = product.price * Number(item.quantity);

                            return (
                                <div className="data-row" key={index}>
                                    <div>
                                        <strong>{product.name}</strong>
                                        <span>{item.quantity} × {formatCurrency(product.price)} · GST {product.gstRate}%</span>
                                    </div>
                                    <div className="data-value">{formatCurrency(amount)}</div>
                                </div>
                            );
                        })}

                        {summary.taxableAmount > 0 && (
                            <div className="invoice-summary">
                                <div className="invoice-summary-inner">
                                    <div className="invoice-summary-row">
                                        <span>Taxable amount</span>
                                        <span>{formatCurrency(summary.taxableAmount)}</span>
                                    </div>
                                    <div className="invoice-summary-row">
                                        <span>CGST</span>
                                        <span>{formatCurrency(summary.cgst)}</span>
                                    </div>
                                    <div className="invoice-summary-row">
                                        <span>SGST</span>
                                        <span>{formatCurrency(summary.sgst)}</span>
                                    </div>
                                    <div className="invoice-summary-row">
                                        <span>IGST</span>
                                        <span>{formatCurrency(summary.igst)}</span>
                                    </div>
                                    <div className="invoice-summary-total">
                                        <div className="invoice-summary-row">
                                            <strong>Grand total</strong>
                                            <strong>{formatCurrency(summary.grandTotal)}</strong>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                </section>
            </div>
        </div>
    );
}

export default CreateInvoice;
