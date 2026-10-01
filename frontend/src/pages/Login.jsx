import { useState } from "react";
import { login, register } from "../services/api";

export default function Login({ onAuthenticated }) {
    const [registering, setRegistering] = useState(false);

    const [form, setForm] = useState({
        name: "",
        businessName: "",
        email: "",
        password: ""
    });

    const [error, setError] = useState("");

    function change(e) {
        setForm({
            ...form,
            [e.target.name]: e.target.value
        });
    }

    async function submit(e) {
        e.preventDefault();

        try {
            const result = registering
                ? await register(form)
                : await login(form);

            localStorage.setItem("gstToken", result.token);
            onAuthenticated();
        } catch (e) {
            setError(
                registering
                    ? "Registration failed. Please check your details."
                    : "Login failed. Check your email and password."
            );
        }
    }

    function toggleMode() {
        setRegistering(!registering);
        setError("");
    }

    return (
        <main className="auth-page">
            <section className="auth-card">
                <div className="eyebrow">GST / BILLING</div>

                <h1>
                    {registering
                        ? "Create your account."
                        : "Welcome back."}
                </h1>

                <h2>
                    {registering
                        ? "Set up your billing workspace."
                        : "Sign in to your billing workspace."}
                </h2>

                <form onSubmit={submit}>
                    {registering && (
                        <>
                            <input
                                name="name"
                                placeholder="Your name"
                                value={form.name}
                                onChange={change}
                                required
                            />

                            <input
                                name="businessName"
                                placeholder="Business name"
                                value={form.businessName}
                                onChange={change}
                                required
                            />
                        </>
                    )}

                    <input
                        type="email"
                        name="email"
                        placeholder="Email address"
                        value={form.email}
                        onChange={change}
                        required
                    />

                    <input
                        type="password"
                        name="password"
                        placeholder="Password"
                        minLength="6"
                        value={form.password}
                        onChange={change}
                        required
                    />

                    <button type="submit">
                        {registering ? "Create account" : "Login"}
                    </button>
                </form>

                {error && (
                    <p className="auth-error">
                        {error}
                    </p>
                )}

                <button
                    type="button"
                    className="auth-toggle"
                    onClick={toggleMode}
                >
                    {registering
                        ? "Already have an account? Login"
                        : "New here? Create an account"}
                </button>
            </section>
        </main>
    );
}
