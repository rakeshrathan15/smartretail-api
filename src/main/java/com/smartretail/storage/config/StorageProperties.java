package com.smartretail.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param localDir   folder on disk for public uploads, served under {@code publicPath}
 * @param privateDir folder on disk for private uploads such as payment screenshots; never served directly
 * @param publicPath URL prefix the public files are served under
 */
@ConfigurationProperties("app.storage")
public record StorageProperties(String localDir, String privateDir, String publicPath) {
}
