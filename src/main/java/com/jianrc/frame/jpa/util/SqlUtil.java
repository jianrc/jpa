package com.jianrc.frame.jpa.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class SqlUtil {
    public static String getIpSql(String ip,String alias,String field){
        if(StringUtils.isNotEmpty(ip)){
            String[] ips = ip.split(";|,|\\s");
            List<String> ipList = new ArrayList<String>(ips.length);
            for (String oneIp : ips) {
                String trim = oneIp.trim();
                if(StringUtils.isNotEmpty(trim)){
                    ipList.add(trim);
                }
            }
            
            if(ipList.size() == 1){
                return alias + "." + field + " like '%" + ipList.get(0) + "%'";
            }else if(ipList.size() > 1){
                List<String> qList = new ArrayList<String>(ipList.size());
                for (String oneIp : ipList) {
                    qList.add("'" + oneIp + "'");
                }
                return alias + "." + field + " in (" + StringUtils.join(qList, ",") + ")";
            }
        }
        return null;
    }
    
    public static String getIpSql(String ip,String alias){
        return getIpSql(ip,alias,"ip");
    }

    public static String joinQuote(String[] ips){
        return joinQuote(ips,",");
    }
    
    public static String joinQuote(String[] ips, String separator){
        List<String> qList = new ArrayList<String>(ips.length);
        for (String ip : ips) {
            qList.add("'" + ip + "'");
        }
        return StringUtils.join(qList,separator);
    }
}
