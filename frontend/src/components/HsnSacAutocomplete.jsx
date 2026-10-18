import { useState, useEffect, useRef } from "react";
import { searchHsnSac, validateHsnSac } from "../services/api";
import Icon from "./Icon";

export default function HsnSacAutocomplete({
    value = "",
    type = "HSN", // "HSN" for Goods, "SAC" for Services
    onChange,
    officialDescription = "",
    disabled = false,
    placeholder = "Search code or description..."
}) {
    const [inputValue, setInputValue] = useState(value || "");
    const [suggestions, setSuggestions] = useState([]);
    const [isOpen, setIsOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const [selectedIndex, setSelectedIndex] = useState(-1);
    const [validationError, setValidationError] = useState("");
    const [verifiedItem, setVerifiedItem] = useState(null);

    const abortControllerRef = useRef(null);
    const containerRef = useRef(null);
    const inputRef = useRef(null);

    // Sync input value with external prop changes
    useEffect(() => {
        setInputValue(value || "");
        if (value && value.trim()) {
            // Validate on initial load or code change
            validateHsnSac(value.trim(), type)
                .then(res => {
                    if (res.valid && res.existsInCatalogue && res.item) {
                        setVerifiedItem(res.item);
                        setValidationError("");
                    } else {
                        setVerifiedItem(null);
                        setValidationError(res.message || `Invalid ${type} code`);
                    }
                })
                .catch(() => {
                    setVerifiedItem(null);
                });
        } else {
            setVerifiedItem(null);
            setValidationError("");
        }
    }, [value, type]);

    // Close dropdown on click outside
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (containerRef.current && !containerRef.current.contains(e.target)) {
                setIsOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    // Perform debounced search
    useEffect(() => {
        if (!isOpen) return;

        const query = inputValue.trim();

        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
        }

        const controller = new AbortController();
        abortControllerRef.current = controller;

        const timer = setTimeout(async () => {
            try {
                setLoading(true);
                const res = await searchHsnSac(query, type, 0, 15, controller.signal);
                setSuggestions(res.content || []);
                setSelectedIndex(-1);
            } catch (err) {
                if (err.name !== "AbortError") {
                    setSuggestions([]);
                }
            } finally {
                setLoading(false);
            }
        }, 250);

        return () => {
            clearTimeout(timer);
            controller.abort();
        };
    }, [inputValue, type, isOpen]);

    const handleInputChange = (e) => {
        const val = e.target.value;
        setInputValue(val);
        setIsOpen(true);
        setValidationError("");
    };

    const handleSelect = (item) => {
        setInputValue(item.code);
        setVerifiedItem(item);
        setValidationError("");
        setIsOpen(false);
        if (onChange) {
            onChange(item.code, item.description, item.gstRate, item.id);
        }
    };

    const handleClear = () => {
        setInputValue("");
        setVerifiedItem(null);
        setValidationError("");
        setSuggestions([]);
        setIsOpen(false);
        if (onChange) {
            onChange("", "", null, null);
        }
        if (inputRef.current) {
            inputRef.current.focus();
        }
    };

    const handleKeyDown = (e) => {
        if (!isOpen) {
            if (e.key === "ArrowDown" || e.key === "ArrowUp") {
                setIsOpen(true);
            }
            return;
        }

        if (e.key === "ArrowDown") {
            e.preventDefault();
            setSelectedIndex(prev => (prev < suggestions.length - 1 ? prev + 1 : 0));
        } else if (e.key === "ArrowUp") {
            e.preventDefault();
            setSelectedIndex(prev => (prev > 0 ? prev - 1 : suggestions.length - 1));
        } else if (e.key === "Enter") {
            e.preventDefault();
            if (selectedIndex >= 0 && selectedIndex < suggestions.length) {
                handleSelect(suggestions[selectedIndex]);
            }
        } else if (e.key === "Escape") {
            setIsOpen(false);
        }
    };

    const displayDescription = verifiedItem?.description || officialDescription;

    return (
        <div className="hsn-autocomplete-container" ref={containerRef} style={{ position: "relative" }}>
            <div className="hsn-input-wrapper" style={{ position: "relative", display: "flex", alignItems: "center" }}>
                <input
                    ref={inputRef}
                    type="text"
                    className={`form-control ${validationError ? "is-invalid" : verifiedItem ? "is-valid" : ""}`}
                    value={inputValue}
                    onChange={handleInputChange}
                    onFocus={() => setIsOpen(true)}
                    onKeyDown={handleKeyDown}
                    placeholder={placeholder || (type === "HSN" ? "e.g. 8471 or Laptop" : "e.g. 9983 or IT Consulting")}
                    disabled={disabled}
                    autoComplete="off"
                    style={{
                        paddingRight: inputValue ? "60px" : "32px",
                        fontWeight: "600",
                        letterSpacing: "0.5px"
                    }}
                />

                {loading ? (
                    <div style={{ position: "absolute", right: "12px", pointerEvents: "none" }}>
                        <div className="spinner" style={{ width: "16px", height: "16px", borderWidth: "2px" }} />
                    </div>
                ) : inputValue ? (
                    <button
                        type="button"
                        onClick={handleClear}
                        title="Clear classification"
                        style={{
                            position: "absolute",
                            right: "10px",
                            background: "transparent",
                            border: "none",
                            color: "var(--text-muted)",
                            cursor: "pointer",
                            padding: "4px",
                            display: "flex",
                            alignItems: "center",
                            borderRadius: "50%"
                        }}
                    >
                        <Icon type="close" size={13} />
                    </button>
                ) : null}
            </div>

            {/* Validation Message / Error Alert */}
            {validationError && (
                <div style={{ fontSize: "11px", color: "var(--danger)", marginTop: "4px", display: "flex", alignItems: "center", gap: "4px" }}>
                    <span>⚠</span>
                    <span>{validationError}</span>
                </div>
            )}

            {/* Official Description Pill / Verification Badge */}
            {displayDescription && !validationError && (
                <div style={{
                    marginTop: "6px",
                    padding: "6px 10px",
                    background: "var(--primary-light)",
                    border: "1px solid rgba(37, 99, 235, 0.2)",
                    borderRadius: "6px",
                    fontSize: "11px",
                    color: "var(--primary-text)",
                    lineHeight: "1.4"
                }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "2px" }}>
                        <strong style={{ fontSize: "10px", textTransform: "uppercase", letterSpacing: "0.5px" }}>
                            Official {type} Classification:
                        </strong>
                        {verifiedItem?.gstRate != null && (
                            <span style={{
                                background: "var(--primary)",
                                color: "#ffffff",
                                padding: "1px 6px",
                                borderRadius: "4px",
                                fontSize: "10px",
                                fontWeight: "700"
                            }}>
                                {verifiedItem.gstRate}% Verified GST
                            </span>
                        )}
                    </div>
                    <div>{displayDescription}</div>
                </div>
            )}

            {/* Autocomplete Dropdown List */}
            {isOpen && (
                <div className="hsn-dropdown-menu" style={{
                    position: "absolute",
                    top: "calc(100% + 4px)",
                    left: 0,
                    right: 0,
                    maxHeight: "260px",
                    overflowY: "auto",
                    backgroundColor: "var(--bg-card)",
                    border: "1px solid var(--border-color)",
                    borderRadius: "8px",
                    boxShadow: "var(--shadow-lg)",
                    zIndex: 1000,
                    padding: "4px"
                }}>
                    <div style={{
                        padding: "6px 10px",
                        fontSize: "10px",
                        fontWeight: "700",
                        textTransform: "uppercase",
                        color: "var(--text-muted)",
                        borderBottom: "1px solid var(--border-color)",
                        display: "flex",
                        justifyContent: "space-between"
                    }}>
                        <span>Official CBIC {type} Master Catalogue</span>
                        <span>{suggestions.length} results</span>
                    </div>

                    {suggestions.length === 0 ? (
                        <div style={{ padding: "12px", textAlign: "center", fontSize: "12px", color: "var(--text-muted)" }}>
                            {loading ? "Searching official catalogue..." : `No active ${type} entries match "${inputValue}"`}
                        </div>
                    ) : (
                        suggestions.map((item, index) => {
                            const isSelected = index === selectedIndex;
                            return (
                                <div
                                    key={item.id}
                                    onClick={() => handleSelect(item)}
                                    onMouseEnter={() => setSelectedIndex(index)}
                                    style={{
                                        padding: "8px 10px",
                                        cursor: "pointer",
                                        borderRadius: "6px",
                                        backgroundColor: isSelected ? "var(--bg-card-hover)" : "transparent",
                                        borderBottom: "1px solid rgba(226, 232, 240, 0.4)",
                                        display: "flex",
                                        flexDirection: "column",
                                        gap: "2px"
                                    }}
                                >
                                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                                        <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                                            <span style={{
                                                fontFamily: "monospace",
                                                fontWeight: "700",
                                                fontSize: "13px",
                                                color: "var(--primary-text)",
                                                letterSpacing: "0.5px"
                                            }}>
                                                {item.code}
                                            </span>
                                            <span style={{
                                                fontSize: "9px",
                                                padding: "1px 5px",
                                                borderRadius: "4px",
                                                background: item.type === "HSN" ? "#e0f2fe" : "#fef3c7",
                                                color: item.type === "HSN" ? "#0369a1" : "#92400e",
                                                fontWeight: "600"
                                            }}>
                                                {item.type} · {item.classificationLevel || "STANDARD"}
                                            </span>
                                        </div>
                                        {item.gstRate != null && (
                                            <span style={{
                                                fontSize: "11px",
                                                fontWeight: "700",
                                                color: "var(--success-text)",
                                                background: "var(--success-bg)",
                                                padding: "1px 6px",
                                                borderRadius: "4px"
                                            }}>
                                                {item.gstRate}% GST
                                            </span>
                                        )}
                                    </div>
                                    <div style={{
                                        fontSize: "11px",
                                        color: "var(--text-secondary)",
                                        lineHeight: "1.3",
                                        whiteSpace: "normal",
                                        overflow: "hidden",
                                        textOverflow: "ellipsis"
                                    }}>
                                        {item.description}
                                    </div>
                                </div>
                            );
                        })
                    )}
                </div>
            )}
        </div>
    );
}
