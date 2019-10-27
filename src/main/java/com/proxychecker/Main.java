package com.proxychecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;

public class Main {

    private static Scanner scanner;
    private static String proxiesPath;
    private static String workingProxiesPath;
    private static String testingURL;
    private static ArrayList<Thread> threads = new ArrayList<>();
    private static ArrayList<String> proxies = new ArrayList<>();

    public static void main(String[] args) {

        disableApacheLogging();
        scanner = new Scanner(System.in);
        
        System.out.println("Introduce your proxy list path. Note that the proxy list must be have one proxy per line. Example: C:\\proxylist.txt");
        proxiesPath = requestFilePath();

        System.out.println("Introduce path of the file to save the results. Example: C:\\workingproxysresults.txt");
        workingProxiesPath = requestFilePath();
        System.out.println("Introduce an URL for testing purposes. Example: https://google.es");
        testingURL = scanner.nextLine();
        System.out.println("Press any key to start.");
        scanner.nextLine();
        System.out.println("STARTED.");

        processProxyList();

        threads.forEach((thread) -> {
            thread.start();
        });

    }

    private static String requestFilePath() {
        boolean valid = false;
        File file = new File(scanner.nextLine());
        while (!valid) {
            if (file.exists()) {
                valid = true;
            } else {
                System.out.println("File not found. Enter again.");
                file = new File(scanner.nextLine());
            }

        }

        return file.getPath();
    }

    private static void processProxyList() {
        try (BufferedReader br = new BufferedReader(new FileReader(new File(proxiesPath)))) {
            String line;
            while ((line = br.readLine()) != null) {
                proxies.add(line);
                String[] datosProxy = line.split(":");

                threads.add(new Thread() {
                    @Override
                    public void run() {
                        sendGetRequest(testingURL, datosProxy[0], Integer.parseInt(datosProxy[1]));
                    }
                });

            }
        } catch (FileNotFoundException ex) {
            System.out.println("The proxy list path you introduced has not found.");
            System.out.println(ex);
            System.exit(0);
        } catch (IOException ex) {
            System.out.println("I/O Exception.");
            System.out.println(ex);
            System.exit(0);
        }
    }

    private static void sendGetRequest(String URL, String ip, int port) {
        try {
            HttpHost proxy = new HttpHost(ip, port);
            HttpClient httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            HttpGet httpGet = new HttpGet(URL);
            httpGet.setHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (en-us) AppleWebKit/534.14 (KHTML, like Gecko; Google Wireless Transcoder) Chrome/9.0.597 Safari/534.14");

            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                saveProxy(ip, port);
            }

        } catch (Exception e) {
        }

    }

    private static void saveProxy(String ip, int port) {
        try {
            String textsave = ip + ":" + port + "\n";

            BufferedReader br = new BufferedReader(new FileReader(workingProxiesPath));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(ip + ":" + port)) {
                    System.out.println("Already added proxy.");
                    return;
                }
            }

            Files.write(Paths.get(workingProxiesPath), textsave.getBytes(), StandardOpenOption.APPEND);
            System.out.println("Proxy added.");
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private static void disableApacheLogging() {
        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");
    }

}
