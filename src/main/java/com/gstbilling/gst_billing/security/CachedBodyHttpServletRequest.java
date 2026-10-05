package com.gstbilling.gst_billing.security;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * An {@link HttpServletRequestWrapper} that caches the HTTP request body in memory,
 * allowing the request body to be read multiple times via {@link #getInputStream()},
 * {@link #getReader()}, or {@link #getCachedBody()}.
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        byte[] body;
        try {
            InputStream is = request.getInputStream();
            body = (is != null) ? is.readAllBytes() : new byte[0];
        } catch (IllegalStateException ex) {
            // In case getReader() was already invoked on the underlying request
            BufferedReader reader = request.getReader();
            if (reader != null) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append(System.lineSeparator());
                }
                String encoding = request.getCharacterEncoding();
                Charset charset = getCharsetOrDefault(encoding);
                body = sb.toString().getBytes(charset);
            } else {
                body = new byte[0];
            }
        }
        this.cachedBody = body;
    }

    public byte[] getCachedBody() {
        return this.cachedBody;
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedServletInputStream(this.cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        Charset charset = getCharsetOrDefault(getCharacterEncoding());
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }

    @Override
    public int getContentLength() {
        return this.cachedBody.length;
    }

    @Override
    public long getContentLengthLong() {
        return this.cachedBody.length;
    }

    private static Charset getCharsetOrDefault(String encoding) {
        if (encoding != null && !encoding.isBlank()) {
            try {
                return Charset.forName(encoding);
            } catch (Exception ignored) {
                return StandardCharsets.UTF_8;
            }
        }
        return StandardCharsets.UTF_8;
    }

    private static class CachedServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream inputStream;

        public CachedServletInputStream(byte[] cachedBody) {
            this.inputStream = new ByteArrayInputStream(cachedBody != null ? cachedBody : new byte[0]);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // Synchronous servlet processing - no async read listener required
        }

        @Override
        public int read() {
            return inputStream.read();
        }

        @Override
        public int read(byte[] b, int off, int len) {
            return inputStream.read(b, off, len);
        }

        @Override
        public int available() {
            return inputStream.available();
        }
    }
}
