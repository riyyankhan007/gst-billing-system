import { useState } from "react";
import Icon from "../components/Icon";
import { login, register, forgotPassword, resetPassword } from "../services/api";

export default function Login({ onAuthenticated, onBackToHome, initialMode = "login" }) {
    // Modes: "login" | "register" | "forgot" | "reset"
    const [mode, setMode] = useState(initialMode);

    const [form, setForm] = useState({
        name: "",
        businessName: "",
        email: "",
        password: "",
        token: "",
        newPassword: "",
        confirmPassword: ""
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [successMessage, setSuccessMessage] = useState("");

    function handleChange(e) {
        setForm({
            ...form,
            [e.target.name]: e.target.value
        });
        if (error) setError("");
    }

    function switchMode(newMode) {
        setMode(newMode);
        setError("");
        setSuccessMessage("");
    }

    async function handleLogin(e) {
        e.preventDefault();
        setLoading(true);
        setError("");

        try {
            const res = await login({
                email: form.email.trim(),
                password: form.password
            });
            localStorage.setItem("gstToken", res.token);
            onAuthenticated();
        } catch (err) {
            setError(err.message || "Invalid email or password");
        } finally {
            setLoading(false);
        }
    }

    async function handleRegister(e) {
        e.preventDefault();
        setLoading(true);
        setError("");

        if (!form.name.trim() || !form.businessName.trim() || !form.email.trim() || !form.password) {
            setError("Please fill in all required fields marked with *");
            setLoading(false);
            return;
        }

        if (form.password.length < 8 || !/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(form.password)) {
            setError("Password must be at least 8 characters and include uppercase, lowercase, and a number");
            setLoading(false);
            return;
        }

        try {
            const res = await register({
                name: form.name.trim(),
                businessName: form.businessName.trim(),
                email: form.email.trim(),
                password: form.password
            });
            localStorage.setItem("gstToken", res.token);
            onAuthenticated();
        } catch (err) {
            setError(err.message || "Registration failed. Please check your details.");
        } finally {
            setLoading(false);
        }
    }

    async function handleForgotPassword(e) {
        e.preventDefault();
        setLoading(true);
        setError("");
        setSuccessMessage("");

        if (!form.email.trim()) {
            setError("Please enter your registered email address");
            setLoading(false);
            return;
        }

        try {
            const res = await forgotPassword({ email: form.email.trim() });
            setSuccessMessage(res.message || "If that email is registered, a 6-digit reset code has been sent!");
            // Automatically switch to reset mode after a brief delay
            setTimeout(() => {
                setMode("reset");
            }, 1200);
        } catch (err) {
            setError(err.message || "Could not process request. Please try again.");
        } finally {
            setLoading(false);
        }
    }

    async function handleResetPassword(e) {
        e.preventDefault();
        setLoading(true);
        setError("");
        setSuccessMessage("");

        if (!form.token.trim()) {
            setError("Please enter the 6-digit reset code");
            setLoading(false);
            return;
        }

        if (!form.newPassword || form.newPassword.length < 6) {
            setError("New password must be at least 6 characters");
            setLoading(false);
            return;
        }

        if (form.newPassword !== form.confirmPassword) {
            setError("Passwords do not match");
            setLoading(false);
            return;
        }

        try {
            const res = await resetPassword({
                email: form.email ? form.email.trim() : undefined,
                token: form.token.trim(),
                newPassword: form.newPassword
            });
            setSuccessMessage(res.message || "Password reset successfully! You can now sign in.");
            setTimeout(() => {
                setForm(prev => ({ ...prev, password: "", token: "", newPassword: "", confirmPassword: "" }));
                setMode("login");
            }, 1800);
        } catch (err) {
            setError(err.message || "Invalid or expired reset code");
        } finally {
            setLoading(false);
        }
    }

    return (
        <main className="auth-page">
            <section className="auth-card">
                {onBackToHome && (
                    <button
                        type="button"
                        className="text-button"
                        style={{ marginBottom: "16px", alignSelf: "flex-start", color: "var(--primary)", fontWeight: "600", display: "inline-flex", alignItems: "center", gap: "6px" }}
                        onClick={onBackToHome}
                    >
                        <Icon type="arrowLeft" size={14} /> Back to Product Overview
                    </button>
                )}
                <div className="auth-header">
                    <div className="auth-logo-badge">GST</div>
                    <h1>
                        {mode === "login" && "Welcome Back"}
                        {mode === "register" && "Create Account"}
                        {mode === "forgot" && "Reset Password"}
                        {mode === "reset" && "New Password"}
                    </h1>
                    <p>
                        {mode === "login" && "Sign in to manage your GST invoices & billing"}
                        {mode === "register" && "Set up your secure business billing workspace"}
                        {mode === "forgot" && "We'll send a 6-digit reset code to your email"}
                        {mode === "reset" && "Enter your verification code and choose a new password"}
                    </p>
                </div>

                {/* Tabs between Login and Register */}
                {(mode === "login" || mode === "register") && (
                    <div className="auth-tabs">
                        <button
                            type="button"
                            className={`auth-tab ${mode === "login" ? "active" : ""}`}
                            onClick={() => switchMode("login")}
                        >
                            Sign In
                        </button>
                        <button
                            type="button"
                            className={`auth-tab ${mode === "register" ? "active" : ""}`}
                            onClick={() => switchMode("register")}
                        >
                            Sign Up
                        </button>
                    </div>
                )}

                {error && <div className="form-error">{error}</div>}
                {successMessage && <div className="form-success">{successMessage}</div>}

                {/* LOGIN FORM */}
                {mode === "login" && (
                    <form onSubmit={handleLogin}>
                        <div className="form-field">
                            <label htmlFor="login-email">
                                Email Address <span className="req-star">*</span>
                            </label>
                            <input
                                id="login-email"
                                type="email"
                                name="email"
                                placeholder="name@company.com"
                                value={form.email}
                                onChange={handleChange}
                                required
                                autoComplete="email"
                            />
                        </div>

                        <div className="form-field">
                            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                                <label htmlFor="login-password">
                                    Password <span className="req-star">*</span>
                                </label>
                                <button
                                    type="button"
                                    className="auth-link"
                                    style={{ fontSize: "12px", fontWeight: "normal" }}
                                    onClick={() => switchMode("forgot")}
                                >
                                    Forgot password?
                                </button>
                            </div>
                            <input
                                id="login-password"
                                type="password"
                                name="password"
                                placeholder="••••••••"
                                value={form.password}
                                onChange={handleChange}
                                required
                                autoComplete="current-password"
                            />
                        </div>

                        <button
                            type="submit"
                            className="primary-button"
                            style={{ width: "100%", marginTop: "8px" }}
                            disabled={loading}
                        >
                            {loading ? "Signing in..." : "Sign In to Workspace"}
                        </button>
                    </form>
                )}

                {/* REGISTER FORM */}
                {mode === "register" && (
                    <form onSubmit={handleRegister}>
                        <div className="form-field">
                            <label htmlFor="reg-name">
                                Your Full Name <span className="req-star">*</span>
                            </label>
                            <input
                                id="reg-name"
                                name="name"
                                placeholder="e.g. John Doe"
                                value={form.name}
                                onChange={handleChange}
                                required
                            />
                        </div>

                        <div className="form-field">
                            <label htmlFor="reg-business">
                                Business / Firm Name <span className="req-star">*</span>
                            </label>
                            <input
                                id="reg-business"
                                name="businessName"
                                placeholder="e.g. Apex Enterprises"
                                value={form.businessName}
                                onChange={handleChange}
                                required
                            />
                        </div>

                        <div className="form-field">
                            <label htmlFor="reg-email">
                                Email Address <span className="req-star">*</span>
                            </label>
                            <input
                                id="reg-email"
                                type="email"
                                name="email"
                                placeholder="name@business.com"
                                value={form.email}
                                onChange={handleChange}
                                required
                            />
                        </div>

                        <div className="form-field">
                            <label htmlFor="reg-password">
                                Password (min. 6 chars) <span className="req-star">*</span>
                            </label>
                            <input
                                id="reg-password"
                                type="password"
                                name="password"
                                placeholder="••••••••"
                                minLength="6"
                                value={form.password}
                                onChange={handleChange}
                                required
                            />
                        </div>

                        <button
                            type="submit"
                            className="primary-button"
                            style={{ width: "100%", marginTop: "8px" }}
                            disabled={loading}
                        >
                            {loading ? "Creating Account..." : "Create Business Account"}
                        </button>
                    </form>
                )}

                {/* FORGOT PASSWORD FORM */}
                {mode === "forgot" && (
                    <form onSubmit={handleForgotPassword}>
                        <div className="form-field">
                            <label htmlFor="forgot-email">
                                Registered Email <span className="req-star">*</span>
                            </label>
                            <input
                                id="forgot-email"
                                type="email"
                                name="email"
                                placeholder="name@company.com"
                                value={form.email}
                                onChange={handleChange}
                                required
                            />
                            <small>We will send a 6-digit verification code to this email.</small>
                        </div>

                        <button
                            type="submit"
                            className="primary-button"
                            style={{ width: "100%", marginTop: "12px" }}
                            disabled={loading}
                        >
                            {loading ? "Sending Code..." : "Send Reset Code"}
                        </button>

                        <div className="auth-footer-links">
                            <button
                                type="button"
                                className="auth-link"
                                onClick={() => switchMode("reset")}
                            >
                                Already have a reset code? Enter it here
                            </button>
                            <button
                                type="button"
                                className="auth-link"
                                style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", gap: "6px" }}
                                onClick={() => switchMode("login")}
                            >
                                <Icon type="arrowLeft" size={13} /> Back to Sign In
                            </button>
                        </div>
                    </form>
                )}

                {/* RESET PASSWORD FORM */}
                {mode === "reset" && (
                    <form onSubmit={handleResetPassword}>
                        <div className="form-field">
                            <label htmlFor="reset-token">
                                Verification Code <span className="req-star">*</span>
                            </label>
                            <input
                                id="reset-token"
                                name="token"
                                placeholder="Enter verification code"
                                value={form.token}
                                onChange={handleChange}
                                maxLength={64}
                                style={{ letterSpacing: "2px", textAlign: "center", fontSize: "16px", fontWeight: "bold" }}
                                required
                            />
                        </div>

                        <div className="form-field">
                            <label htmlFor="reset-new-password">
                                New Password <span className="req-star">*</span>
                            </label>
                            <input
                                id="reset-new-password"
                                type="password"
                                name="newPassword"
                                placeholder="Minimum 6 characters"
                                minLength="6"
                                value={form.newPassword}
                                onChange={handleChange}
                                required
                            />
                        </div>

                        <div className="form-field">
                            <label htmlFor="reset-confirm-password">
                                Confirm New Password <span className="req-star">*</span>
                            </label>
                            <input
                                id="reset-confirm-password"
                                type="password"
                                name="confirmPassword"
                                placeholder="Re-enter new password"
                                minLength="6"
                                value={form.confirmPassword}
                                onChange={handleChange}
                                required
                            />
                        </div>

                        <button
                            type="submit"
                            className="primary-button"
                            style={{ width: "100%", marginTop: "12px" }}
                            disabled={loading}
                        >
                            {loading ? "Updating Password..." : "Reset Password & Login"}
                        </button>

                        <div className="auth-footer-links">
                            <button
                                type="button"
                                className="auth-link"
                                onClick={() => switchMode("forgot")}
                            >
                                Didn't receive a code? Request again
                            </button>
                            <button
                                type="button"
                                className="auth-link"
                                style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", gap: "6px" }}
                                onClick={() => switchMode("login")}
                            >
                                <Icon type="arrowLeft" size={13} /> Back to Sign In
                            </button>
                        </div>
                    </form>
                )}

                {/* Footer toggle for login/register */}
                {(mode === "login" || mode === "register") && (
                    <div className="auth-footer-links">
                        <p style={{ fontSize: "13px", color: "var(--text-muted)" }}>
                            {mode === "login" ? "New to GST Billing? " : "Already have an account? "}
                            <button
                                type="button"
                                className="auth-link"
                                onClick={() => switchMode(mode === "login" ? "register" : "login")}
                            >
                                {mode === "login" ? "Create an account" : "Sign in here"}
                            </button>
                        </p>
                    </div>
                )}
            </section>
        </main>
    );
}
