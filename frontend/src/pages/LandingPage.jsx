import React, { useState } from "react";
import Icon from "../components/Icon";
import dashboardLaptopImg from "../assets/gst-dashboard-laptop.jpg";

export default function LandingPage({ onLogin, onRegister }) {
    // Interactive live GST calculator on the landing page
    const [calcAmount, setCalcAmount] = useState(50000);
    const [calcGstRate, setCalcGstRate] = useState(18);
    const [calcInterstate, setCalcInterstate] = useState(false);
    const [activeFaq, setActiveFaq] = useState(null);

    // Calculated amounts
    const taxable = Number(calcAmount) || 0;
    const rate = Number(calcGstRate) || 0;
    const totalGst = (taxable * rate) / 100;
    const cgst = calcInterstate ? 0 : totalGst / 2;
    const sgst = calcInterstate ? 0 : totalGst / 2;
    const igst = calcInterstate ? totalGst : 0;
    const grandTotal = taxable + totalGst;

    const fmt = val =>
        new Intl.NumberFormat("en-IN", {
            style: "currency",
            currency: "INR",
            maximumFractionDigits: 2
        }).format(val || 0);

    return (
        <div className="landing-container">
            {/* Top Navigation Bar */}
            <header className="landing-nav">
                <div className="landing-nav-inner">
                    <div className="landing-brand">
                        <div className="landing-brand-logo">GST</div>
                        <div className="landing-brand-title">
                            <strong>GST Pro</strong>
                            <span>Billing & ERP Cloud</span>
                        </div>
                    </div>

                    <nav className="landing-nav-links">
                        <a href="#features">Features</a>
                        <a href="#calculator">Live GST Calc</a>
                        <a href="#workflow">How It Works</a>
                        <a href="#security">Security</a>
                        <a href="#faq">FAQ</a>
                    </nav>

                    <div className="landing-nav-actions">
                        <button
                            type="button"
                            className="landing-btn-ghost"
                            onClick={onLogin}
                        >
                            Sign In
                        </button>
                        <button
                            type="button"
                            className="landing-btn-primary"
                            style={{ display: "inline-flex", alignItems: "center", gap: "6px" }}
                            onClick={onRegister}
                        >
                            Get Started Free <Icon type="arrowRight" size={14} />
                        </button>
                    </div>
                </div>
            </header>

            {/* HERO SECTION */}
            <section className="landing-hero">
                {/* Hero Showcase Graphic: Laptop with GST Pro Dashboard */}
                <div className="landing-hero-image-wrapper">
                    <div className="laptop-glow-effect" />
                    <img
                        src={dashboardLaptopImg}
                        alt="GST Pro Software Dashboard on Laptop"
                        className="landing-hero-laptop-image"
                    />
                    <div className="floating-stat-pill stat-1">
                        <span className="stat-label">Total Revenue</span>
                        <strong className="stat-val">₹12,45,000</strong>
                    </div>
                    <div className="floating-stat-pill stat-2">
                        <span className="stat-label">Tax Saved (ITC)</span>
                        <strong className="stat-val" style={{ color: "#10b981" }}>₹1,98,500</strong>
                    </div>
                </div>

                <div className="landing-hero-content">
                    <div className="landing-badge-pill">
                        <span className="pulse-dot" />
                        <span>Official GST 2026 Ready · Multi-Tenant Architecture</span>
                    </div>

                    <h1 className="landing-hero-headline">
                        Smart GST Invoicing, Instant WhatsApp Reminders &
                        <span className="text-gradient"> Complete Tax ERP</span>
                    </h1>

                    <p className="landing-hero-subtitle">
                        Empower your business with lightning-fast tax invoicing, automated CGST/SGST/IGST math,
                        Input Tax Credit (ITC) reconciliation, stock audits, and one-click WhatsApp payment reminders.
                    </p>

                    <div className="landing-hero-ctas">
                        <button
                            type="button"
                            className="landing-cta-primary"
                            onClick={onRegister}
                        >
                            <span>Start 14-Day Free Trial</span>
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" width="18" height="18">
                                <line x1="5" y1="12" x2="19" y2="12" />
                                <polyline points="12 5 19 12 12 19" />
                            </svg>
                        </button>
                        <button
                            type="button"
                            className="landing-cta-secondary"
                            onClick={onLogin}
                        >
                            <span>Access Workspace</span>
                        </button>
                    </div>

                    <div className="landing-hero-trust">
                        <div className="trust-item">
                            <span className="trust-icon"><Icon type="check" size={14} color="#10b981" /></span>
                            <span>100% GST & E-Way Ready</span>
                        </div>
                        <div className="trust-item">
                            <span className="trust-icon"><Icon type="check" size={14} color="#10b981" /></span>
                            <span>Multi-Tenant Data Isolation</span>
                        </div>
                        <div className="trust-item">
                            <span className="trust-icon"><Icon type="check" size={14} color="#10b981" /></span>
                            <span>Instant WhatsApp Reminders</span>
                        </div>
                    </div>
                </div>
            </section>

            {/* KEY FEATURES SECTION */}
            <section id="features" className="landing-section">
                <div className="section-title-wrap">
                    <span className="section-eyebrow">ENTERPRISE-GRADE CAPABILITIES</span>
                    <h2 className="section-title">Everything Indian Businesses Need for GST Billing</h2>
                    <p className="section-desc">
                        Built from the ground up with Spring Boot security and high-speed React for ultra-reliable operations.
                    </p>
                </div>

                <div className="feature-grid">
                    <div className="feature-card">
                        <div className="feature-icon" style={{ background: "rgba(37, 99, 235, 0.1)", color: "#2563eb" }}>
                            <Icon type="zap" size={24} />
                        </div>
                        <h3>Tax Invoices in 10 Seconds</h3>
                        <p>
                            Generate flawless tax invoices with auto-populated HSN/SAC codes, instant multi-item line math, and PDF generation.
                        </p>
                    </div>

                    <div className="feature-card">
                        <div className="feature-icon" style={{ background: "rgba(37, 211, 102, 0.1)", color: "#25d366" }}>
                            <Icon type="whatsapp" size={24} />
                        </div>
                        <h3>WhatsApp & Email Reminders</h3>
                        <p>
                            Recover outstanding receivables up to 3x faster with 1-click WhatsApp payment reminders with invoice PDF attachments.
                        </p>
                    </div>

                    <div className="feature-card">
                        <div className="feature-icon" style={{ background: "rgba(99, 102, 241, 0.1)", color: "#6366f1" }}>
                            <Icon type="shield" size={24} />
                        </div>
                        <h3>Tenant-Level Security & Privacy</h3>
                        <p>
                            Your business financials, customer list, and turnover remain 100% confidential with rigorous cryptographic tenant isolation.
                        </p>
                    </div>

                    <div className="feature-card">
                        <div className="feature-icon" style={{ background: "rgba(16, 185, 129, 0.1)", color: "#10b981" }}>
                            <Icon type="report" size={24} />
                        </div>
                        <h3>Inward Bills & ITC Tracking</h3>
                        <p>
                            Log vendor bills, claim Input Tax Credit (ITC) with full compliance, and minimize your net GST liability automatically.
                        </p>
                    </div>

                    <div className="feature-card">
                        <div className="feature-icon" style={{ background: "rgba(245, 158, 11, 0.1)", color: "#f59e0b" }}>
                            <Icon type="inventory" size={24} />
                        </div>
                        <h3>Automated Stock Audit Trail</h3>
                        <p>
                            Stock quantities update seamlessly when you issue sales invoices or record purchases, with low-stock alerts.
                        </p>
                    </div>

                    <div className="feature-card">
                        <div className="feature-icon" style={{ background: "rgba(239, 68, 68, 0.1)", color: "#ef4444" }}>
                            <Icon type="notes" size={24} />
                        </div>
                        <h3>Credit & Debit Notes</h3>
                        <p>
                            Handle sales returns, price adjustments, and post-sale discounts with automatic invoice balance corrections.
                        </p>
                    </div>
                </div>
            </section>

            {/* INTERACTIVE GST CALCULATOR WIDGET */}
            <section id="calculator" className="landing-section bg-alt">
                <div className="section-title-wrap">
                    <span className="section-eyebrow">TRY IT RIGHT NOW</span>
                    <h2 className="section-title">Interactive GST Tax Calculator</h2>
                    <p className="section-desc">
                        Experience our real-time GST computation engine right on this page.
                    </p>
                </div>

                <div className="calculator-wrapper">
                    <div className="calculator-card">
                        <div className="calculator-inputs">
                            <div className="form-field">
                                <label>Taxable Amount (₹)</label>
                                <input
                                    type="number"
                                    className="input"
                                    value={calcAmount}
                                    onChange={e => setCalcAmount(Number(e.target.value))}
                                    min="0"
                                    step="1000"
                                />
                            </div>

                            <div className="form-field">
                                <label>Select GST Slab</label>
                                <div className="rate-selector">
                                    {[0, 5, 12, 18, 28].map(r => (
                                        <button
                                            key={r}
                                            type="button"
                                            className={`rate-btn ${calcGstRate === r ? "active" : ""}`}
                                            onClick={() => setCalcGstRate(r)}
                                        >
                                            {r}%
                                        </button>
                                    ))}
                                </div>
                            </div>

                            <div className="form-field">
                                <label style={{ display: "flex", alignItems: "center", gap: "8px", cursor: "pointer" }}>
                                    <input
                                        type="checkbox"
                                        checked={calcInterstate}
                                        onChange={e => setCalcInterstate(e.target.checked)}
                                        style={{ width: "18px", height: "18px" }}
                                    />
                                    <span>Inter-state Transaction (IGST applicable)</span>
                                </label>
                            </div>
                        </div>

                        <div className="calculator-results">
                            <h4 style={{ margin: "0 0 16px", color: "var(--text-primary)" }}>Calculation Summary</h4>

                            <div className="result-row">
                                <span>Taxable Value:</span>
                                <strong>{fmt(taxable)}</strong>
                            </div>

                            {!calcInterstate ? (
                                <>
                                    <div className="result-row">
                                        <span>Central GST (CGST {rate / 2}%):</span>
                                        <strong>{fmt(cgst)}</strong>
                                    </div>
                                    <div className="result-row">
                                        <span>State GST (SGST {rate / 2}%):</span>
                                        <strong>{fmt(sgst)}</strong>
                                    </div>
                                </>
                            ) : (
                                <div className="result-row">
                                    <span>Integrated GST (IGST {rate}%):</span>
                                    <strong>{fmt(igst)}</strong>
                                </div>
                            )}

                            <div className="result-row total">
                                <span>Grand Invoice Total:</span>
                                <strong style={{ color: "#2563eb", fontSize: "20px" }}>{fmt(grandTotal)}</strong>
                            </div>

                            <button
                                type="button"
                                className="primary-button"
                                style={{ width: "100%", marginTop: "20px", display: "inline-flex", alignItems: "center", justifyContent: "center", gap: "6px" }}
                                onClick={onRegister}
                            >
                                Generate Real Invoices with This Rate <Icon type="arrowRight" size={14} />
                            </button>
                        </div>
                    </div>
                </div>
            </section>

            {/* HOW IT WORKS / 3-STEP WORKFLOW */}
            <section id="workflow" className="landing-section">
                <div className="section-title-wrap">
                    <span className="section-eyebrow">SIMPLE 3-STEP ONBOARDING</span>
                    <h2 className="section-title">From Signup to First Invoice in 60 Seconds</h2>
                    <p className="section-desc">No complicated installations or accounting degrees required.</p>
                </div>

                <div className="steps-grid">
                    <div className="step-card">
                        <div className="step-number">01</div>
                        <h3>Create Your Workspace</h3>
                        <p>Enter your business name, GSTIN, and state. Your multi-tenant secure database is created instantly.</p>
                    </div>

                    <div className="step-card">
                        <div className="step-number">02</div>
                        <h3>Add Items & Customers</h3>
                        <p>Easily input catalog items with HSN codes and customer GSTINs. Stock is tracked in real-time.</p>
                    </div>

                    <div className="step-card">
                        <div className="step-number">03</div>
                        <h3>Issue & Get Paid</h3>
                        <p>Download GST-compliant PDF invoices, record payments, and send instant WhatsApp reminders.</p>
                    </div>
                </div>
            </section>

            {/* FREQUENTLY ASKED QUESTIONS */}
            <section id="faq" className="landing-section bg-alt">
                <div className="section-title-wrap">
                    <span className="section-eyebrow">GOT QUESTIONS?</span>
                    <h2 className="section-title">Frequently Asked Questions</h2>
                </div>

                <div className="faq-container">
                    {[
                        {
                            q: "Is my business data isolated and secure from other users?",
                            a: "Yes! Every single database query filters by your unique Tenant ID. Users cannot access or view another business's customers, turnover, or invoices under any circumstance."
                        },
                        {
                            q: "How does the WhatsApp payment reminder feature work?",
                            a: "When you click 'Remind' on any unpaid or overdue invoice, the platform pre-formats a polite reminder message with your invoice details and opens the official WhatsApp Web/API link with zero setup."
                        },
                        {
                            q: "Does this handle both Intra-State (CGST+SGST) and Inter-State (IGST)?",
                            a: "Absolutely. The system automatically detects whether the supplier and customer are in the same state or across state borders and applies either CGST+SGST or IGST accordingly."
                        },
                        {
                            q: "Can I export data for GSTR-1 and GSTR-3B filing?",
                            a: "Yes. Head to the Reports tab at any time to export CSV records for sales, invoices, customers, and products formatted for your Chartered Accountant (CA)."
                        }
                    ].map((item, idx) => (
                        <div
                            key={idx}
                            className={`faq-item ${activeFaq === idx ? "open" : ""}`}
                            onClick={() => setActiveFaq(activeFaq === idx ? null : idx)}
                        >
                            <div className="faq-question">
                                <span>{item.q}</span>
                                <span className="faq-chevron">
                                    {activeFaq === idx ? <Icon type="chevronUp" size={14} /> : <Icon type="chevronDown" size={14} />}
                                </span>
                            </div>
                            {activeFaq === idx && <div className="faq-answer">{item.a}</div>}
                        </div>
                    ))}
                </div>
            </section>

            {/* CALL TO ACTION BANNER */}
            <section className="landing-cta-banner">
                <div className="cta-banner-inner">
                    <h2>Ready to Streamline Your GST Billing & Compliance?</h2>
                    <p>Join thousands of Indian enterprises and MSMEs powering their invoices with GST Pro.</p>
                    <div style={{ display: "flex", gap: "16px", justifyContent: "center", flexWrap: "wrap", marginTop: "24px" }}>
                        <button
                            type="button"
                            className="landing-cta-primary"
                            onClick={onRegister}
                            style={{ background: "#ffffff", color: "#2563eb", boxShadow: "0 8px 20px rgba(0,0,0,0.2)", display: "inline-flex", alignItems: "center", gap: "6px" }}
                        >
                            Start Free Trial Today <Icon type="arrowRight" size={14} />
                        </button>
                        <button
                            type="button"
                            className="landing-cta-secondary"
                            onClick={onLogin}
                            style={{ borderColor: "rgba(255,255,255,0.4)", color: "#ffffff" }}
                        >
                            Sign In to Account
                        </button>
                    </div>
                </div>
            </section>

            {/* FOOTER */}
            <footer className="landing-footer">
                <div className="landing-footer-inner">
                    <div className="footer-col">
                        <div className="landing-brand">
                            <div className="landing-brand-logo">GST</div>
                            <div className="landing-brand-title">
                                <strong style={{ color: "#ffffff" }}>GST Pro</strong>
                                <span style={{ color: "#94a3b8" }}>Enterprise Billing ERP</span>
                            </div>
                        </div>
                        <p style={{ marginTop: "12px", fontSize: "13px", color: "#94a3b8", maxWidth: "300px" }}>
                            Next-generation GST billing, ITC accounting, and real-time receivables platform for Indian businesses.
                        </p>
                    </div>

                    <div className="footer-col">
                        <h4>Platform</h4>
                        <a href="#features">Invoicing</a>
                        <a href="#features">WhatsApp Reminders</a>
                        <a href="#calculator">GST Calculator</a>
                        <a href="#features">ITC Purchases</a>
                    </div>

                    <div className="footer-col">
                        <h4>Security & Trust</h4>
                        <a href="#security">Multi-Tenant Isolation</a>
                        <a href="#security">Spring Security JWT</a>
                        <a href="#security">GST Council Compliant</a>
                    </div>

                    <div className="footer-col">
                        <h4>Get Started</h4>
                        <button type="button" className="btn btn-primary" onClick={onRegister} style={{ width: "100%", marginBottom: "8px" }}>
                            Create Free Account
                        </button>
                        <button type="button" className="btn btn-secondary" onClick={onLogin} style={{ width: "100%" }}>
                            Sign In
                        </button>
                    </div>
                </div>

                <div className="footer-bottom">
                    <span>© {new Date().getFullYear()} GST Billing & SaaS ERP. All rights reserved. Compliant with Goods and Services Tax Act.</span>
                </div>
            </footer>
        </div>
    );
}
