package com.phuong.proxy;

import java.util.List;

public class ProxyInternet implements Internet {
    private Internet internet = new RealInternet();
    private static List<String> restrictedSites;

    static {
        restrictedSites = List.of("abc.com", "def.com", "ghi.com");
    }

    @Override
    public void connectTo(String host) throws Exception {
        if (restrictedSites.contains(host.toLowerCase())) {
            throw new Exception("Access Denied");
        }

        internet.connectTo(host);
    }
}
