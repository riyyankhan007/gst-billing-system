package com.gstbilling.gst_billing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvc configuration. Static direct serving of uploads directory has been removed
 * in favor of authenticated, tenant-isolated serving via UploadController.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    // Static resource handler for uploads removed for security.
    // Uploads are now served via UploadController with authentication & tenant verification.
}
