package proxy;

import android.content.Context;

import org.littleshoot.proxy.mitm.Authority;
import org.littleshoot.proxy.mitm.CertificateSniffingMitmManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import timber.log.Timber;

public class ProxyUtils {


    private static ConcurrentHashMap responseMap = new ConcurrentHashMap<>();

    public static String generateCertificate(File file) {
        Timber.d(file.getAbsolutePath());
        if(file.exists()){
            file.delete();
        }
        file.mkdirs();

        Timber.d("generateCertificate");
        Authority authority = new Authority(file, "heimdall-proxy", "changeit".toCharArray(), "Heimdall", "HeimdallOrga", "HeimdallOrgaUnit", "HeimdallCert", "HeimdallCertUnit");
        try {
            new CertificateSniffingMitmManager(authority);
            return getStringFromFile(new File(file, "heimdall-proxy.pem").getPath());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }

    private static String getStringFromFile(String filePath) throws Exception {
        Timber.d("getStringFromFile");
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    private static String convertStreamToString(InputStream is) throws Exception {
        Timber.d("convertStreamToString");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }


    public static HeimdallHttpProxyServer startProxyService(File file, Context applicationContext) {
        try {
            Timber.d("startProxyService");
            Authority authority = new Authority(file, "heimdall-proxy", "changeit".toCharArray(), "Heimdall", "HeimdallOrga", "HeimdallOrgaUnit", "HeimdallCert", "HeimdallCertUnit");
            HeimdallHttpProxyServer server = new HeimdallHttpProxyServer(new InetSocketAddress("127.0.0.1",9090),new CertificateSniffingMitmManager(authority), applicationContext);
            server.start();
            return server;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
