package com.gstbilling.gst_billing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.TimeZone;

@SpringBootTest
class GstBillingApplicationTests {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

	@Test
	void contextLoads() {
	}

}
