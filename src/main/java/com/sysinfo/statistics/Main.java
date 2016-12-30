package com.sysinfo.statistics;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class Main {
    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/info", new InfoHandler());
            server.createContext("/latencies", new LatenciesHandler());
            server.setExecutor(null);
            server.start();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    public static class InfoHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            JSONObject obj = respond();
            String jsonString = obj.toJSONString();
            t.sendResponseHeaders(200, jsonString.length());
            OutputStream os = t.getResponseBody();
            os.write(jsonString.getBytes());
            os.close();
        }

        public JSONObject respond() {
            Sigar sigar = new Sigar();
            JSONObject obj = new JSONObject();
            double[] averageLoad;
            try {
                Thread.sleep(1);
                CpuPerc perc = sigar.getCpuPerc();
                averageLoad = sigar.getLoadAverage();
                if (averageLoad.length == 3) {
                    obj.put("recentLoad", new Double(averageLoad[0]));
                    obj.put("minLoad", new Double(averageLoad[1]));
                    obj.put("fiveMinsLoad", new Double(averageLoad[2]));
                    obj.put("user", new Double(perc.getUser()));
                    obj.put("system", new Double(perc.getSys()));
                    obj.put("nice", new Double(perc.getNice()));
                    obj.put("wait", new Double(perc.getWait()));
                    obj.put("idle", new Double(perc.getIdle()));
                }
            } catch (Exception e) {
                System.out.println(e.toString());
            }
            Mem m = new Mem();
            try {
                m = sigar.getMem();
            } catch (Exception e) {
                System.out.println(e.toString());
            }
            obj.put("freeMem", new Double(m.getActualFree()));
            obj.put("usedMemory", new Double(m.getActualUsed()));
            obj.put("usedMemPercent", new Double(m.getUsedPercent()));
            obj.put("time", new Long(System.currentTimeMillis()));
            return obj;
        }
    }

    public static class LatenciesHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            JSONArray obj = respond();
            String jsonString = obj.toJSONString();
            t.sendResponseHeaders(200, jsonString.length());
            OutputStream os = t.getResponseBody();
            os.write(jsonString.getBytes());
            os.close();
        }

        public JSONArray respond() {
            // split name by - output-time-" + context.getStormId()
            final File folder = new File("/var/latencies/");
            JSONArray obj = new JSONArray();
            File[] files = folder.listFiles();
            try {
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];

                    if (!file.isDirectory()) {
                        BufferedReader br = null;
                        String line = "";
                        String cvsSplitBy = ",";
                        try {
                            br = new BufferedReader(new FileReader(file));
                            while ((line = br.readLine()) != null) {
                                if (line.length() != 0) {
                                    String[] split_line = line.split(cvsSplitBy);
                                    String topology = split_line[0];
                                    String spout = split_line[1];
                                    String sink = split_line[2];
                                    String latency = (split_line[3]);
                                    JSONObject t = new JSONObject();
                                    t.put("topology", topology);
                                    t.put("spout", spout);
                                    t.put("sink", sink);
                                    t.put("latency", latency);
                                    obj.add(t);
                                }
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (br != null) {
                                try {
                                    br.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Exception: " + e.toString());
                e.printStackTrace();
            }
            return obj;
        }
    }
}