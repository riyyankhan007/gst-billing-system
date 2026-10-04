package com.gstbilling.gst_billing;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone;

@SpringBootApplication
public class GstBillingApplication {

    static {
        // PostgreSQL 17 does not accept the deprecated 'Asia/Calcutta' timezone ID sent by Windows JVMs.
        // Modernize to 'Asia/Kolkata' to ensure seamless JDBC startup handshake.
        if ("Asia/Calcutta".equalsIgnoreCase(TimeZone.getDefault().getID())) {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        }
    }

    @PostConstruct
    public void init() {
        if ("Asia/Calcutta".equalsIgnoreCase(TimeZone.getDefault().getID())) {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        }
    }

    public static void main(String[] args) {
        if ("Asia/Calcutta".equalsIgnoreCase(TimeZone.getDefault().getID())) {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        }
        SpringApplication.run(GstBillingApplication.class, args);
    }

}
