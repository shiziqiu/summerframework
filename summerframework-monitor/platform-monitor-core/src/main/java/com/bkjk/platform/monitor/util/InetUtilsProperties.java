
package com.bkjk.platform.monitor.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(InetUtilsProperties.PREFIX)
public class InetUtilsProperties {
    public static final String PREFIX = "spring.cloud.inetutils";

    public static String getPREFIX() {
        return PREFIX;
    }

    private String defaultHostname = "localhost";

    private String defaultIpAddress = "127.0.0.1";

    @Value("${spring.util.timeout.sec:${SPRING_UTIL_TIMEOUT_SEC:1}}")
    private int timeoutSeconds = 1;

    private List<String> ignoredInterfaces = new ArrayList<>();

    private boolean useOnlySiteLocalInterfaces = false;

    private List<String> preferredNetworks = new ArrayList<>();

    public String getDefaultHostname() {
        return defaultHostname;
    }

    public String getDefaultIpAddress() {
        return defaultIpAddress;
    }

    public List<String> getIgnoredInterfaces() {
        return ignoredInterfaces;
    }

    public List<String> getPreferredNetworks() {
        return preferredNetworks;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public boolean isUseOnlySiteLocalInterfaces() {
        return useOnlySiteLocalInterfaces;
    }

    public void setDefaultHostname(String defaultHostname) {
        this.defaultHostname = defaultHostname;
    }

    public void setDefaultIpAddress(String defaultIpAddress) {
        this.defaultIpAddress = defaultIpAddress;
    }

    public void setIgnoredInterfaces(List<String> ignoredInterfaces) {
        this.ignoredInterfaces = ignoredInterfaces;
    }

    public void setPreferredNetworks(List<String> preferredNetworks) {
        this.preferredNetworks = preferredNetworks;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public void setUseOnlySiteLocalInterfaces(boolean useOnlySiteLocalInterfaces) {
        this.useOnlySiteLocalInterfaces = useOnlySiteLocalInterfaces;
    }
}
