import React, { useState, useEffect, useRef } from "react";
import Icon from "./Icon";
import { getUserBusinesses, switchBusiness, createBusiness } from "../services/api";
import { Skeleton } from "./ui/Skeleton";

const INDIAN_STATES = [
    { code: "27", name: "Maharashtra" },
    { code: "07", name: "Delhi" },
    { code: "29", name: "Karnataka" },
    { code: "24", name: "Gujarat" },
    { code: "33", name: "Tamil Nadu" },
    { code: "36", name: "Telangana" },
    { code: "09", name: "Uttar Pradesh" },
    { code: "19", name: "West Bengal" },
    { code: "06", name: "Haryana" },
    { code: "08", name: "Rajasthan" },
    { code: "23", name: "Madhya Pradesh" },
    { code: "10", name: "Bihar" },
    { code: "37", name: "Andhra Pradesh" },
    { code: "03", name: "Punjab" },
    { code: "21", name: "Odisha" },
    { code: "32", name: "Kerala" },
    { code: "20", name: "Jharkhand" },
    { code: "18", name: "Assam" },
    { code: "22", name: "Chhattisgarh" },
    { code: "05", name: "Uttarakhand" },
    { code: "01", name: "Jammu and Kashmir" },
    { code: "02", name: "Himachal Pradesh" },
    { code: "30", name: "Goa" },
    { code: "04", name: "Chandigarh" },
    { code: "34", name: "Puducherry" },
    { code: "38", name: "Ladakh" }
];

export default function BusinessSwitcher({ profile, onBusinessSwitched }) {
    const [open, setOpen] = useState(false);
    const [businesses, setBusinesses] = useState([]);
    const [loading, setLoading] = useState(false);
    const [switching, setSwitching] = useState(false);
    const [error, setError] = useState("");
    const [showCreateModal, setShowCreateModal] = useState(false);

    // Create Business form state
    const [newName, setNewName] = useState("");
    const [newGstin, setNewGstin] = useState("");
    const [newState, setNewState] = useState("");
    const [newStateCode, setNewStateCode] = useState("");
    const [newAddress, setNewAddress] = useState("");
    const [newPrefix, setNewPrefix] = useState("INV");
    const [createLoading, setCreateLoading] = useState(false);
    const [createError, setCreateError] = useState("");

    const dropdownRef = useRef(null);

    useEffect(() => {
        if (open) {
            loadBusinesses();
        }
    }, [open]);

    // Close on click outside
    useEffect(() => {
        function handleClickOutside(event) {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setOpen(false);
            }
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    // Close on Escape key
    useEffect(() => {
        function handleKeyDown(e) {
            if (e.key === "Escape") {
                if (showCreateModal) {
                    setShowCreateModal(false);
                } else if (open) {
                    setOpen(false);
                }
            }
        }
        document.addEventListener("keydown", handleKeyDown);
        return () => document.removeEventListener("keydown", handleKeyDown);
    }, [open, showCreateModal]);

    async function loadBusinesses() {
        try {
            setLoading(true);
            setError("");
            const list = await getUserBusinesses();
            setBusinesses(list || []);
        } catch (err) {
            setError(err.message || "Failed to load businesses");
        } finally {
            setLoading(false);
        }
    }

    async function handleSwitch(biz) {
        if (biz.businessId === profile?.businessId) {
            setOpen(false);
            return;
        }

        try {
            setSwitching(true);
            const res = await switchBusiness(biz.businessId);
            if (res && res.token) {
                localStorage.setItem("gstToken", res.token);
            }
            setOpen(false);
            if (onBusinessSwitched) {
                await onBusinessSwitched(res);
            }
            await loadBusinesses();
        } catch (err) {
            setError(err.message || "Failed to switch business");
        } finally {
            setSwitching(false);
        }
    }

    function handleGstinChange(val) {
        const uppercaseVal = val.toUpperCase().replace(/[^A-Z0-9]/g, "");
        setNewGstin(uppercaseVal);
        if (uppercaseVal.length >= 2) {
            const code = uppercaseVal.substring(0, 2);
            const matched = INDIAN_STATES.find(s => s.code === code);
            if (matched && !newState) {
                setNewState(matched.name);
                setNewStateCode(matched.code);
            }
        }
    }

    function handleStateChange(val) {
        setNewState(val);
        const matched = INDIAN_STATES.find(s => s.name.toLowerCase() === val.toLowerCase());
        if (matched) {
            setNewStateCode(matched.code);
        } else {
            setNewStateCode("");
        }
    }

    async function handleCreateBusiness(e) {
        e.preventDefault();
        if (!newName.trim()) {
            setCreateError("Business legal name is required (*)");
            return;
        }

        try {
            setCreateLoading(true);
            setCreateError("");
            const res = await createBusiness({
                name: newName.trim(),
                gstin: newGstin.trim() || null,
                state: newState.trim() || null,
                stateCode: newStateCode.trim() || null,
                address: newAddress.trim() || null,
                invoicePrefix: newPrefix.trim() || "INV"
            });

            if (res && res.token) {
                localStorage.setItem("gstToken", res.token);
            }

            setShowCreateModal(false);
            setOpen(false);
            setNewName("");
            setNewGstin("");
            setNewState("");
            setNewStateCode("");
            setNewAddress("");
            setNewPrefix("INV");

            if (onBusinessSwitched) {
                await onBusinessSwitched(res);
            }
            await loadBusinesses();
        } catch (err) {
            setCreateError(err.message || "Failed to create business entity");
        } finally {
            setCreateLoading(false);
        }
    }

    const currentBizName = profile?.businessName || "Select Business";
    const currentRole = profile?.role || "OWNER";
    const currentGstin = profile?.businessGstin;

    return (
        <div className="business-switcher-wrapper" ref={dropdownRef}>
            {/* Header Switcher Trigger Button */}
            <button
                type="button"
                className={`business-switcher-btn ${open ? "is-active" : ""}`}
                onClick={() => setOpen(!open)}
                title="Switch active business account"
                aria-expanded={open}
                aria-haspopup="true"
            >
                <div className="business-switcher-avatar">
                    {currentBizName.charAt(0).toUpperCase()}
                </div>
                <div className="business-switcher-info">
                    <div className="business-switcher-row">
                        <span className="business-switcher-name" title={currentBizName}>
                            {currentBizName}
                        </span>
                        <span className="business-switcher-role-badge">
                            {currentRole}
                        </span>
                    </div>
                    <div className="business-switcher-sub">
                        {currentGstin ? `GSTIN: ${currentGstin}` : "Unregistered Business"}
                    </div>
                </div>
                <svg
                    className={`business-switcher-chevron ${open ? "is-open" : ""}`}
                    width="14"
                    height="14"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                >
                    <polyline points="6 9 12 15 18 9" />
                </svg>
            </button>

            {/* Dropdown Menu */}
            {open && (
                <div className="business-switcher-menu" role="menu">
                    <div className="business-switcher-menu-header">
                        <span>Authorized Businesses</span>
                        <span className="business-switcher-menu-count">{businesses.length}</span>
                    </div>

                    {switching && (
                        <div style={{
                            padding: "8px 12px",
                            margin: "4px 0",
                            background: "#eff6ff",
                            color: "#1d4ed8",
                            borderRadius: "6px",
                            fontSize: "12px",
                            fontWeight: 600,
                            textAlign: "center",
                            border: "1px solid #bfdbfe"
                        }}>
                            Switching business context...
                        </div>
                    )}

                    {error && (
                        <div style={{
                            padding: "8px 12px",
                            margin: "4px 0",
                            background: "#fef2f2",
                            color: "#991b1b",
                            borderRadius: "6px",
                            fontSize: "12px",
                            border: "1px solid #fecaca"
                        }}>
                            {error}
                        </div>
                    )}

                    <div className="business-switcher-menu-list">
                        {loading ? (
                            <div style={{ padding: "8px 12px", display: "flex", flexDirection: "column", gap: "8px" }}>
                                {[1, 2].map(i => (
                                    <div key={i} style={{ display: "flex", alignItems: "center", gap: "10px", padding: "6px 8px" }}>
                                        <Skeleton width="32px" height="32px" borderRadius="8px" />
                                        <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: "6px" }}>
                                            <Skeleton width="60%" height="13px" />
                                            <Skeleton width="40%" height="10px" />
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : businesses.length === 0 ? (
                            <div style={{ padding: "16px", textAlign: "center", fontSize: "12px", color: "var(--text-muted)" }}>
                                No businesses available
                            </div>
                        ) : (
                            businesses.map((biz) => {
                                const isSelected = biz.businessId === profile?.businessId;
                                return (
                                    <button
                                        key={biz.businessId}
                                        type="button"
                                        role="menuitem"
                                        onClick={() => handleSwitch(biz)}
                                        disabled={switching}
                                        className={`business-switcher-item ${isSelected ? "is-selected" : ""}`}
                                    >
                                        <div className="business-switcher-item-left">
                                            <div className="business-switcher-item-avatar">
                                                {biz.businessName ? biz.businessName.charAt(0).toUpperCase() : "B"}
                                            </div>
                                            <div className="business-switcher-item-text">
                                                <div className="business-switcher-item-name">
                                                    {biz.businessName}
                                                </div>
                                                <div className="business-switcher-item-sub">
                                                    {biz.gstin ? `GST: ${biz.gstin}` : "Unregistered"} • {biz.role || "MEMBER"}
                                                </div>
                                            </div>
                                        </div>
                                        {isSelected && (
                                            <svg
                                                width="16"
                                                height="16"
                                                viewBox="0 0 24 24"
                                                fill="none"
                                                stroke="#2563eb"
                                                strokeWidth="2.5"
                                                strokeLinecap="round"
                                                strokeLinejoin="round"
                                                style={{ flexShrink: 0, marginLeft: "8px" }}
                                            >
                                                <polyline points="20 6 9 17 4 12" />
                                            </svg>
                                        )}
                                    </button>
                                );
                            })
                        )}
                    </div>

                    <div className="business-switcher-menu-footer">
                        <button
                            type="button"
                            className="business-switcher-create-btn"
                            onClick={() => {
                                setOpen(false);
                                setShowCreateModal(true);
                            }}
                        >
                            <Icon type="plus" size={13} />
                            <span>Create New Business</span>
                        </button>
                    </div>
                </div>
            )}

            {/* Create Business Modal */}
            {showCreateModal && (
                <div
                    className="modal-backdrop"
                    onClick={() => setShowCreateModal(false)}
                    role="dialog"
                    aria-modal="true"
                    aria-labelledby="create-biz-title"
                >
                    <div
                        className="modal-content"
                        onClick={(e) => e.stopPropagation()}
                        style={{ maxWidth: "520px", width: "100%" }}
                    >
                        <div className="modal-header">
                            <div>
                                <h2 id="create-biz-title">
                                    <span style={{
                                        width: "30px",
                                        height: "30px",
                                        borderRadius: "6px",
                                        background: "linear-gradient(135deg, #2563eb, #1d4ed8)",
                                        color: "#ffffff",
                                        display: "inline-flex",
                                        alignItems: "center",
                                        justifyContent: "center",
                                        marginRight: "6px",
                                        flexShrink: 0
                                    }}>
                                        <Icon type="building" size={16} color="#ffffff" />
                                    </span>
                                    <span>Create New Business</span>
                                </h2>
                                <p className="modal-subtitle">
                                    Add a separate GST registration, legal entity, or branch company
                                </p>
                            </div>
                            <button
                                type="button"
                                className="modal-close-btn"
                                onClick={() => setShowCreateModal(false)}
                                title="Close modal"
                            >
                                <Icon type="close" size={14} />
                            </button>
                        </div>

                        <form onSubmit={handleCreateBusiness}>
                            <div className="modal-body" style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                                {createError && (
                                    <div style={{
                                        color: "var(--danger-text)",
                                        backgroundColor: "var(--danger-bg)",
                                        border: "1px solid #fecaca",
                                        borderRadius: "8px",
                                        padding: "10px 14px",
                                        fontSize: "13px",
                                        display: "flex",
                                        alignItems: "center",
                                        gap: "8px"
                                    }}>
                                        <Icon type="alert" size={16} color="var(--danger)" />
                                        <span>{createError}</span>
                                    </div>
                                )}

                                <div className="form-field" style={{ margin: 0 }}>
                                    <label>
                                        Business Legal Name <span className="req-star">*</span>
                                    </label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        placeholder="e.g. Acme Enterprises Pvt Ltd"
                                        value={newName}
                                        onChange={(e) => setNewName(e.target.value)}
                                        required
                                        autoFocus
                                    />
                                    <small>Official trade name or registered entity title</small>
                                </div>

                                <div className="form-field" style={{ margin: 0 }}>
                                    <label>GSTIN (Optional)</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        placeholder="22AAAAA0000A1Z5"
                                        value={newGstin}
                                        onChange={(e) => handleGstinChange(e.target.value)}
                                        maxLength={15}
                                    />
                                    <small>15-digit GSTIN (state code is automatically detected)</small>
                                </div>

                                <div className="form-grid-2" style={{ gap: "12px" }}>
                                    <div className="form-field" style={{ margin: 0 }}>
                                        <label>State / Union Territory</label>
                                        <select
                                            className="form-control"
                                            value={newState}
                                            onChange={(e) => handleStateChange(e.target.value)}
                                        >
                                            <option value="">Select State / UT</option>
                                            {INDIAN_STATES.map((s) => (
                                                <option key={s.code} value={s.name}>
                                                    {s.name} ({s.code})
                                                </option>
                                            ))}
                                        </select>
                                    </div>

                                    <div className="form-field" style={{ margin: 0 }}>
                                        <label>Invoice Prefix</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            placeholder="INV"
                                            value={newPrefix}
                                            onChange={(e) => setNewPrefix(e.target.value.toUpperCase())}
                                            maxLength={10}
                                        />
                                        <small>e.g. INV, BIZ, TX</small>
                                    </div>
                                </div>

                                <div className="form-field" style={{ margin: 0 }}>
                                    <label>Principal Business Address</label>
                                    <textarea
                                        className="form-control"
                                        placeholder="Building, Plot No., Street, City, PIN"
                                        rows={3}
                                        value={newAddress}
                                        onChange={(e) => setNewAddress(e.target.value)}
                                    />
                                </div>
                            </div>

                            <div className="modal-footer">
                                <button
                                    type="button"
                                    className="secondary-button"
                                    onClick={() => setShowCreateModal(false)}
                                    disabled={createLoading}
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    className={`primary-button ${createLoading ? "btn-loading" : ""}`}
                                    disabled={createLoading}
                                    style={{ display: "inline-flex", alignItems: "center", gap: "8px" }}
                                >
                                    {createLoading && <span className="inline-spinner" />}
                                    <span>{createLoading ? "Creating Business..." : "Create & Switch"}</span>
                                    {!createLoading && (
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                                            <polyline points="9 18 15 12 9 6" />
                                        </svg>
                                    )}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
