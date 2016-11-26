package com.sysinfo.statistics;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import org.json.simple.JSONObject;


public class Main {
    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/info", new InfoHandler());
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
                    obj.put("fiveMinsLoad", new Double (averageLoad[2]));
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
}