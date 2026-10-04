import { useEffect, useState } from "react";
import {
    getBusiness,
    updateBusiness,
    uploadLogo,
    uploadSignature,
    changePassword,
    getBusinessUsers,
    createBusinessUser,
    updateUserRole
} from "../services/api";

export default function BusinessSettings() {
    const [tab, setTab] = useState("profile"); // profile, bank, invoice, team, security
    const [business, setBusiness] = useState(null);
    const [saving, setSaving] = useState(false);
    const [uploadingLogo, setUploadingLogo] = useState(false);
    const [uploadingSig, setUploadingSig] = useState(false);
    const [message, setMessage] = useState({ text: "", type: "" });

    // Team state
    const [users, setUsers] = useState([]);
    const [teamLoading, setTeamLoading] = useState(false);
    const [showInviteModal, setShowInviteModal] = useState(false);
    const [newUserName, setNewUserName] = useState("");
    const [newUserEmail, setNewUserEmail] = useState("");
    const [newUserPassword, setNewUserPassword] = useState("");
    const [newUserRole, setNewUserRole] = useState("ACCOUNTANT");
    const [inviteSubmitting, setInviteSubmitting] = useState(false);

    // Password state
    const [pwForm, setPwForm] = useState({ currentPassword: "", newPassword: "", confirmPassword: "" });
    const [pwLoading, setPwLoading] = useState(false);
    const [pwMessage, setPwMessage] = useState({ text: "", type: "" });

    useEffect(() => {
        loadBusiness();
        loadTeam();
    }, []);

    async function loadBusiness() {
        try {
            const data = await getBusiness();
            setBusiness(data);
        } catch (err) {
            setMessage({ text: err.message || "Failed to load business profile", type: "error" });
        }
    }

    async function loadTeam() {
        try {
            setTeamLoading(true);
            const list = await getBusinessUsers();
            setUsers(list);
        } catch (err) {
            console.error("Team load error:", err);
        } finally {
            setTeamLoading(false);
        }
    }

    const handleChange = e => {
        setBusiness({
            ...business,
            [e.target.name]: e.target.value
        });
    };

    const handleSaveBusiness = async e => {
        e.preventDefault();
        setSaving(true);
        setMessage({ text: "", type: "" });

        if (!business.name?.trim()) {
            setMessage({ text: "Business Name is required (*)", type: "error" });
            setSaving(false);
            return;
        }

        try {
            const payload = {
                ...business,
                stateCode: business.stateCode ? String(business.stateCode) : null
            };
            const updated = await updateBusiness(payload);
            setBusiness(updated);
            setMessage({ text: "Business settings saved successfully!", type: "success" });
            setTimeout(() => setMessage({ text: "", type: "" }), 4000);
        } catch (err) {
            setMessage({ text: err.message || "Could not save business details", type: "error" });
        } finally {
            setSaving(false);
        }
    };

    const handleLogoUpload = async e => {
        const file = e.target.files?.[0];
        if (!file) return;

        setUploadingLogo(true);
        setMessage({ text: "", type: "" });

        try {
            const updated = await uploadLogo(file);
            setBusiness(updated);
            setMessage({ text: "Logo updated successfully!", type: "success" });
        } catch (err) {
            setMessage({ text: err.message || "Could not upload logo", type: "error" });
        } finally {
            setUploadingLogo(false);
        }
    };

    const handleSignatureUpload = async e => {
        const file = e.target.files?.[0];
        if (!file) return;

        setUploadingSig(true);
        setMessage({ text: "", type: "" });

        try {
            const updated = await uploadSignature(file);
            setBusiness(updated);
            setMessage({ text: "Signature / Stamp updated successfully!", type: "success" });
        } catch (err) {
            setMessage({ text: err.message || "Could not upload signature", type: "error" });
        } finally {
            setUploadingSig(false);
        }
    };

    const handleInviteUser = async e => {
        e.preventDefault();
        try {
            setInviteSubmitting(true);
            await createBusinessUser({
                name: newUserName,
                email: newUserEmail,
                password: newUserPassword,
                role: newUserRole
            });
            setShowInviteModal(false);
            setNewUserName("");
            setNewUserEmail("");
            setNewUserPassword("");
            setMessage({ text: "Team member added successfully!", type: "success" });
            setTimeout(() => setMessage({ text: "", type: "" }), 4000);
            await loadTeam();
        } catch (err) {
            alert(err.message || "Failed to add team member");
        } finally {
            setInviteSubmitting(false);
        }
    };

    const handleRoleChange = async (userId, newRole) => {
        try {
            await updateUserRole(userId, newRole);
            setMessage({ text: "Role updated successfully!", type: "success" });
            setTimeout(() => setMessage({ text: "", type: "" }), 4000);
            await loadTeam();
        } catch (err) {
            alert(err.message || "Failed to update role");
        }
    };

    const handlePasswordChange = async e => {
        e.preventDefault();
        setPwLoading(true);
        setPwMessage({ text: "", type: "" });

        if (!pwForm.currentPassword) {
            setPwMessage({ text: "Current password is required (*)", type: "error" });
            setPwLoading(false);
            return;
        }
        if (!pwForm.newPassword || pwForm.newPassword.length < 8) {
            setPwMessage({ text: "New password must be at least 8 characters (*)", type: "error" });
            setPwLoading(false);
            return;
        }
        if (pwForm.newPassword !== pwForm.confirmPassword) {
            setPwMessage({ text: "Passwords do not match", type: "error" });
            setPwLoading(false);
            return;
        }

        try {
            await changePassword({
                currentPassword: pwForm.currentPassword,
                newPassword: pwForm.newPassword
            });
            setPwMessage({ text: "Password changed successfully!", type: "success" });
            setPwForm({ currentPassword: "", newPassword: "", confirmPassword: "" });
        } catch (err) {
            setPwMessage({ text: err.message || "Failed to change password", type: "error" });
        } finally {
            setPwLoading(false);
        }
    };

    if (!business) {
        return (
            <div className="card" style={{ padding: "40px", textAlign: "center" }}>
                <div className="spinner" style={{ margin: "0 auto 12px" }} />
                <p>Loading business profile...</p>
            </div>
        );
    }

    return (
        <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            <div className="page-heading">
                <div>
                    <span className="eyebrow">ADMINISTRATION & CONFIG</span>
                    <h1>Business & SaaS Settings</h1>
                    <p>Configure GST profile, banking details, invoice sequences, and team permissions</p>
                </div>
            </div>

            {message.text && (
                <div style={{
                    padding: "12px 16px",
                    borderRadius: "8px",
                    background: message.type === "success" ? "rgba(16, 185, 129, 0.15)" : "rgba(239, 68, 68, 0.15)",
                    color: message.type === "success" ? "#065f46" : "var(--danger)",
                    fontWeight: "600"
                }}>
                    {message.type === "success" ? "✓ " : "✕ "}{message.text}
                </div>
            )}

            {/* Sub-navigation tabs */}
            <div style={{ display: "flex", gap: "10px", borderBottom: "1px solid var(--border)", paddingBottom: "8px", flexWrap: "wrap" }}>
                <button
                    className={`btn ${tab === "profile" ? "btn-primary" : "btn-secondary"}`}
                    onClick={() => setTab("profile")}
                >
                    Business Profile
                </button>
                <button
                    className={`btn ${tab === "bank" ? "btn-primary" : "btn-secondary"}`}
                    onClick={() => setTab("bank")}
                >
                    Bank & UPI Details
                </button>
                <button
                    className={`btn ${tab === "invoice" ? "btn-primary" : "btn-secondary"}`}
                    onClick={() => setTab("invoice")}
                >
                    Invoice Numbering & Terms
                </button>
                <button
                    className={`btn ${tab === "team" ? "btn-primary" : "btn-secondary"}`}
                    onClick={() => setTab("team")}
                >
                    Users & Roles ({users.length})
                </button>
                <button
                    className={`btn ${tab === "security" ? "btn-primary" : "btn-secondary"}`}
                    onClick={() => setTab("security")}
                >
                    Security
                </button>
            </div>

            {/* TAB 1: BUSINESS PROFILE */}
            {tab === "profile" && (
                <form onSubmit={handleSaveBusiness} className="card" style={{ padding: "24px", display: "flex", flexDirection: "column", gap: "18px" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "16px" }}>
                        <h3 style={{ margin: 0, fontSize: "18px" }}>Company Information</h3>
                        <button type="submit" className="btn btn-primary" disabled={saving}>
                            {saving ? "Saving..." : "Save Profile"}
                        </button>
                    </div>

                    {/* Logo & Basic Info */}
                    <div style={{ display: "flex", gap: "20px", alignItems: "center", flexWrap: "wrap", borderBottom: "1px solid var(--border)", paddingBottom: "16px" }}>
                        <div style={{ width: "90px", height: "90px", border: "1px dashed var(--border)", borderRadius: "8px", display: "flex", alignItems: "center", justifyContent: "center", overflow: "hidden", background: "var(--bg-subtle, #f8fafc)" }}>
                            {business.logo ? (
                                <img src={business.logo} alt="Logo" style={{ width: "100%", height: "100%", objectFit: "contain" }} />
                            ) : (
                                <span style={{ fontSize: "11px", color: "var(--muted)", textAlign: "center" }}>No Logo</span>
                            )}
                        </div>
                        <div>
                            <label className="btn btn-secondary btn-sm" style={{ cursor: "pointer" }}>
                                {uploadingLogo ? "Uploading..." : "Upload Logo"}
                                <input type="file" accept="image/*" onChange={handleLogoUpload} style={{ display: "none" }} />
                            </label>
                            <div style={{ fontSize: "11px", color: "var(--muted)", marginTop: "4px" }}>PNG, JPG or WEBP (Max 2MB)</div>
                        </div>
                    </div>

                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
                        <div>
                            <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                Business / Company Name *
                            </label>
                            <input
                                type="text"
                                name="name"
                                className="input"
                                value={business.name || ""}
                                onChange={handleChange}
                                required
                            />
                        </div>
                        <div>
                            <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                GSTIN (15-digit) *
                            </label>
                            <input
                                type="text"
                                name="gstin"
                                maxLength="15"
                                className="input"
                                value={business.gstin || ""}
                                onChange={e => handleChange({ target: { name: "gstin", value: e.target.value.toUpperCase() } })}
                                placeholder="e.g. 27ABCDE1234F1Z5"
                            />
                        </div>
                    </div>

                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
                        <div>
                            <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                PAN Number (10-digit)
                            </label>
                            <input
                                type="text"
                                name="pan"
                                maxLength="10"
                                className="input"
                                value={business.pan || ""}
                                onChange={e => handleChange({ target: { name: "pan", value: e.target.value.toUpperCase() } })}
                                placeholder="e.g. ABCDE1234F"
                            />
                        </div>
                        <div>
                            <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                State *
                            </label>
                            <input
                                type="text"
                                name="state"
                                className="input"
                                value={business.state || ""}
                                onChange={handleChange}
                                required
                            />
                        </div>
                    </div>

                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "16px" }}>
                        <div>
                            <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                State Code
                            </label>
                            <input
                                type="text"
                                name="stateCode"
                                className="input"
                                value={business.stateCode || ""}
                                onChange={handleChange}
                                placeholder="e.g. 27"
                            />
                        </div>
                        <div>
                            <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                Phone
                            </label>
                            <input
                                type="text"
                                name="phone"
                                className="input"
                                value={business.phone || ""}
                                onChange={handleChange}
                            />
                        </div>
                        <div>
                            <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                Email
                            </label>
                            <input
                                type="email"
                                name="email"
                                className="input"
                                value={business.email || ""}
                                onChange={handleChange}
                            />
                        </div>
                    </div>

                    <div>
                        <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                            Office / Registered Address
                        </label>
                        <textarea
                            name="address"
                            className="input"
                            rows="2"
                            value={business.address || ""}
                            onChange={handleChange}
                        />
                    </div>

                    <div>
                        <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                            Website URL
                        </label>
                        <input
                            type="text"
                            name="website"
                            className="input"
                            value={business.website || ""}
                            onChange={handleChange}
                            placeholder="https://mybusiness.com"
                        />
                    </div>
                </form>
            )}

            {/* TAB 2: BANK & UPI DETAILS */}
            {tab === "bank" && (
                <form onSubmit={handleSaveBusiness} className="card" style={{ padding: "24px", display: "flex", flexDirection: "column", gap: "18px" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                        <h3 style={{ margin: 0, fontSize: "18px" }}>Bank & UPI Payment Details</h3>
                        <button type="submit" className="btn btn-primary" disabled={saving}>
                            {saving ? "Saving..." : "Save Bank Details"}
                        </button>
                    </div>
                    <p style={{ margin: 0, fontSize: "13px", color: "var(--muted)" }}>
                        These details are printed automatically on PDF invoices so clients can pay via NEFT/IMPS or UPI QR.
                    </p>

                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
                        <div>
                            <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                Bank Name
                            </label>
                            <input
                                type="text"
                                name="bankName"
                                className="input"
                                value={business.bankName || ""}
                                onChange={handleChange}
                                placeholder="e.g. State Bank of India, HDFC Bank"
                            />
                        </div>
                        <div>
                            <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                Bank Account Number
                            </label>
                            <input
                                type="text"
                                name="bankAccountNumber"
                                className="input"
                                value={business.bankAccountNumber || ""}
                                onChange={handleChange}
                                placeholder="e.g. 50100234567890"
                            />
                        </div>
                    </div>

                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
                        <div>
                            <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                IFSC Code (11 characters)
                            </label>
                            <input
                                type="text"
                                name="bankIfsc"
                                maxLength="11"
                                className="input"
                                value={business.bankIfsc || ""}
                                onChange={e => handleChange({ target: { name: "bankIfsc", value: e.target.value.toUpperCase() } })}
                                placeholder="e.g. SBIN0001234"
                            />
                        </div>
                        <div>
                            <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                UPI ID (VPA)
                            </label>
                            <input
                                type="text"
                                name="upiId"
                                className="input"
                                value={business.upiId || ""}
                                onChange={handleChange}
                                placeholder="e.g. company@okhdfcbank"
                            />
                        </div>
                    </div>

                    {/* Signature / Stamp Upload */}
                    <div style={{ borderTop: "1px solid var(--border)", paddingTop: "16px" }}>
                        <h4 style={{ margin: "0 0 10px", fontSize: "15px" }}>Authorized Signatory / Stamp</h4>
                        <div style={{ display: "flex", gap: "20px", alignItems: "center" }}>
                            <div style={{ width: "120px", height: "60px", border: "1px dashed var(--border)", borderRadius: "6px", display: "flex", alignItems: "center", justifyContent: "center", background: "var(--bg-subtle, #f8fafc)" }}>
                                {business.signature ? (
                                    <img src={business.signature} alt="Signature" style={{ width: "100%", height: "100%", objectFit: "contain" }} />
                                ) : (
                                    <span style={{ fontSize: "11px", color: "var(--muted)" }}>No Stamp</span>
                                )}
                            </div>
                            <label className="btn btn-secondary btn-sm" style={{ cursor: "pointer" }}>
                                {uploadingSig ? "Uploading..." : "Upload Stamp / Signature"}
                                <input type="file" accept="image/*" onChange={handleSignatureUpload} style={{ display: "none" }} />
                            </label>
                        </div>
                    </div>
                </form>
            )}

            {/* TAB 3: INVOICE NUMBERING & TERMS */}
            {tab === "invoice" && (
                <form onSubmit={handleSaveBusiness} className="card" style={{ padding: "24px", display: "flex", flexDirection: "column", gap: "18px" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                        <h3 style={{ margin: 0, fontSize: "18px" }}>Invoice Numbering & Default Terms</h3>
                        <button type="submit" className="btn btn-primary" disabled={saving}>
                            {saving ? "Saving..." : "Save Settings"}
                        </button>
                    </div>

                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "16px" }}>
                        <div>
                            <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                Invoice Prefix *
                            </label>
                            <input
                                type="text"
                                name="invoicePrefix"
                                className="input"
                                value={business.invoicePrefix || ""}
                                onChange={handleChange}
                                placeholder="e.g. INV"
                                required
                            />
                        </div>
                        <div>
                            <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                Financial Year *
                            </label>
                            <input
                                type="text"
                                name="financialYear"
                                className="input"
                                value={business.financialYear || ""}
                                onChange={handleChange}
                                placeholder="e.g. 2026-27"
                                required
                            />
                        </div>
                        <div>
                            <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                Next Sequence Number
                            </label>
                            <input
                                type="number"
                                name="invoiceSeqNumber"
                                className="input"
                                value={business.invoiceSeqNumber || 1}
                                onChange={handleChange}
                            />
                        </div>
                    </div>

                    <div style={{ background: "var(--bg-subtle, #f8fafc)", padding: "12px", borderRadius: "8px", fontSize: "13px" }}>
                        <strong>Generated Invoice Number Preview:</strong>{" "}
                        <code style={{ fontSize: "14px", color: "var(--primary)", fontWeight: "700" }}>
                            {business.invoicePrefix || "INV"}/{business.financialYear || "2026-27"}/{String(business.invoiceSeqNumber || 1).padStart(3, "0")}
                        </code>
                    </div>

                    <div>
                        <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                            Default Terms & Conditions
                        </label>
                        <textarea
                            name="defaultTerms"
                            className="input"
                            rows="4"
                            value={business.defaultTerms || ""}
                            onChange={handleChange}
                            placeholder="1. Goods once sold will not be taken back.&#10;2. Interest @ 18% p.a. will be charged after due date.&#10;3. Subject to local jurisdiction only."
                        />
                    </div>
                </form>
            )}

            {/* TAB 4: USERS & ROLES */}
            {tab === "team" && (
                <div className="card" style={{ padding: "24px", display: "flex", flexDirection: "column", gap: "16px" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                        <div>
                            <h3 style={{ margin: 0, fontSize: "18px" }}>Team Members & Access Roles</h3>
                            <p style={{ margin: "2px 0 0", fontSize: "13px", color: "var(--muted)" }}>
                                Granular permissions: ADMIN, ACCOUNTANT, SALES, and VIEWER
                            </p>
                        </div>
                        <button className="btn btn-primary" onClick={() => setShowInviteModal(true)}>
                            + Add Team Member
                        </button>
                    </div>

                    {teamLoading ? (
                        <p>Loading users...</p>
                    ) : (
                        <table className="table" style={{ width: "100%", margin: 0 }}>
                            <thead>
                                <tr>
                                    <th>Name</th>
                                    <th>Email</th>
                                    <th>Role</th>
                                    <th>Permissions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {users.map(u => (
                                    <tr key={u.id}>
                                        <td style={{ fontWeight: "600" }}>{u.name}</td>
                                        <td>{u.email}</td>
                                        <td>
                                            <select
                                                className="input"
                                                style={{ maxWidth: "160px", padding: "4px 8px", fontSize: "13px" }}
                                                value={u.role}
                                                onChange={e => handleRoleChange(u.id, e.target.value)}
                                            >
                                                <option value="ADMIN">ADMIN</option>
                                                <option value="ACCOUNTANT">ACCOUNTANT</option>
                                                <option value="SALES">SALES</option>
                                                <option value="VIEWER">VIEWER</option>
                                            </select>
                                        </td>
                                        <td style={{ fontSize: "12px", color: "var(--muted)" }}>
                                            {u.role === "ADMIN" && "Full administrative control"}
                                            {u.role === "ACCOUNTANT" && "Invoices, Payments, Reports, Purchases"}
                                            {u.role === "SALES" && "Customers, Invoices, Products"}
                                            {u.role === "VIEWER" && "Read-only analytics access"}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}

                    {/* Invite Modal */}
                    {showInviteModal && (
                        <div className="modal-backdrop">
                            <div className="modal-card" style={{ maxWidth: "450px" }}>
                                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "14px" }}>
                                    <h3 style={{ margin: 0, fontSize: "18px" }}>Add New Team Member</h3>
                                    <button className="btn-close" onClick={() => setShowInviteModal(false)}>✕</button>
                                </div>
                                <form onSubmit={handleInviteUser} style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                                    <div>
                                        <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                            Full Name *
                                        </label>
                                        <input
                                            type="text"
                                            className="input"
                                            value={newUserName}
                                            onChange={e => setNewUserName(e.target.value)}
                                            placeholder="e.g. Rahul Sharma"
                                            required
                                        />
                                    </div>
                                    <div>
                                        <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                            Email Address *
                                        </label>
                                        <input
                                            type="email"
                                            className="input"
                                            value={newUserEmail}
                                            onChange={e => setNewUserEmail(e.target.value)}
                                            placeholder="rahul@company.com"
                                            required
                                        />
                                    </div>
                                    <div>
                                        <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                            Temporary Password *
                                        </label>
                                        <input
                                            type="password"
                                            className="input"
                                            value={newUserPassword}
                                            onChange={e => setNewUserPassword(e.target.value)}
                                            placeholder="Min 8 characters"
                                            required
                                        />
                                    </div>
                                    <div>
                                        <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                            Role *
                                        </label>
                                        <select
                                            className="input"
                                            value={newUserRole}
                                            onChange={e => setNewUserRole(e.target.value)}
                                        >
                                            <option value="ADMIN">ADMIN</option>
                                            <option value="ACCOUNTANT">ACCOUNTANT</option>
                                            <option value="SALES">SALES</option>
                                            <option value="VIEWER">VIEWER</option>
                                        </select>
                                    </div>
                                    <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "8px" }}>
                                        <button type="button" className="btn btn-secondary" onClick={() => setShowInviteModal(false)}>
                                            Cancel
                                        </button>
                                        <button type="submit" className="btn btn-primary" disabled={inviteSubmitting}>
                                            {inviteSubmitting ? "Adding..." : "Add Member"}
                                        </button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    )}
                </div>
            )}

            {/* TAB 5: SECURITY */}
            {tab === "security" && (
                <form onSubmit={handlePasswordChange} className="card" style={{ padding: "24px", maxWidth: "500px", display: "flex", flexDirection: "column", gap: "16px" }}>
                    <h3 style={{ margin: 0, fontSize: "18px" }}>Change Account Password</h3>

                    {pwMessage.text && (
                        <div style={{
                            padding: "10px 14px",
                            borderRadius: "6px",
                            background: pwMessage.type === "success" ? "rgba(16, 185, 129, 0.15)" : "rgba(239, 68, 68, 0.15)",
                            color: pwMessage.type === "success" ? "#065f46" : "var(--danger)",
                            fontSize: "13px"
                        }}>
                            {pwMessage.text}
                        </div>
                    )}

                    <div>
                        <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                            Current Password *
                        </label>
                        <input
                            type="password"
                            className="input"
                            value={pwForm.currentPassword}
                            onChange={e => setPwForm({ ...pwForm, currentPassword: e.target.value })}
                            required
                        />
                    </div>

                    <div>
                        <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                            New Password *
                        </label>
                        <input
                            type="password"
                            className="input"
                            value={pwForm.newPassword}
                            onChange={e => setPwForm({ ...pwForm, newPassword: e.target.value })}
                            required
                        />
                    </div>

                    <div>
                        <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                            Confirm New Password *
                        </label>
                        <input
                            type="password"
                            className="input"
                            value={pwForm.confirmPassword}
                            onChange={e => setPwForm({ ...pwForm, confirmPassword: e.target.value })}
                            required
                        />
                    </div>

                    <button type="submit" className="btn btn-primary" disabled={pwLoading} style={{ alignSelf: "flex-start" }}>
                        {pwLoading ? "Updating..." : "Update Password"}
                    </button>
                </form>
            )}
        </div>
    );
}