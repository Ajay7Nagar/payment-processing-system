package com.example.payments.infra.web;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.util.StreamUtils;

public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    public static final String ATTRIBUTE_CACHED_BODY = CachedBodyHttpServletRequest.class.getName() + ".RAW_BODY";

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
        String charset = request.getCharacterEncoding() != null ? request.getCharacterEncoding() : StandardCharsets.UTF_8.name();
        super.setAttribute(ATTRIBUTE_CACHED_BODY, new String(this.cachedBody, charset));
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
        return new ServletInputStream() {
            @Override
            public int read() {
                return byteArrayInputStream.read();
            }

            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // no-op
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    public static Optional<String> extractBody(HttpServletRequest request) {
        Object attr = request.getAttribute(ATTRIBUTE_CACHED_BODY);
        if (attr instanceof String body) {
            return Optional.of(body);
        }
        return Optional.empty();
    }
}
