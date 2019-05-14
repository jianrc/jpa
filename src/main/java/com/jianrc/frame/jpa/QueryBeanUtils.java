package com.jianrc.frame.jpa;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

public class QueryBeanUtils {
    public static String getValueSeparatedSql(String tableAlias,String column,String str,ColumnShip ship){
        if(StringUtils.isNotEmpty(str)){
            String[] tokens = str.split("\\s");
            Set<String> valids = new LinkedHashSet<String>();
            for (String string : tokens) {
                String trim = string.trim();
                if(StringUtils.isNotEmpty(trim)){
                    valids.add(string);
                }
            }
            if(valids.size() == 0){
                return null;
            }else if(valids.size() == 1){
                String v = valids.iterator().next();
                if(ship == ColumnShip.LIKE || ship == ColumnShip.NOTLIKE){
                    v = '%' + v + "%";
                }
                return tableAlias + "." + column + " " + ship.sign() + " '" + v + "'";
            }else{
                if(ship == ColumnShip.EQUAL){
                    return tableAlias + "." + column + " in (" + QueryBeanUtils.joinQuoted(valids, ",") + ")";
                }else{
                    StringBuilder sb = new StringBuilder("(");
                    for (String v : valids) {
                        if(ship == ColumnShip.LIKE || ship == ColumnShip.NOTLIKE){
                            v = '%' + v + "%";
                        }
                        sb.append(tableAlias + "." + column + " " + ship.sign() + " '" + v + "' or ");
                    }
                    sb.delete(sb.length() - 3, sb.length());
                    sb.append(")");
                    return sb.toString();
                }
            }
        }
        return null;
    }
    public static String getDateFromSqlIf(String tableAlias,String column,String dateFrom){
        if(StringUtils.isNotEmpty(dateFrom)){
            return tableAlias + "." + column + " >= '" + dateFrom + "'";
        }
        return null;
    }
    
    public static String getDateToSqlIf(String tableAlias,String column,String dateTo){
        return getDateToSqlIf(tableAlias,column,dateTo,"yyyy-MM-dd");
    }
    
    public static String getDateToSqlIf(String tableAlias,String column,String dateTo,String format){
        if(StringUtils.isNotEmpty(dateTo)){
            try {
                String dateTo1 = DateFormatUtils.format(DateUtils.addDays(DateUtils.parseDate(dateTo, new String[]{format}),1),format);
                return tableAlias + "." + column + " < '" + dateTo1 + "'";
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    public static String joinQuoted(Collection<String> list,String separator){
        List<String> qlist = new ArrayList<String>();
        for (String string : list) {
            qlist.add("'" + string + "'");
        }
        return StringUtils.join(qlist, separator);
    }
    
    public static String joinQuoted(String[] list,String separator){
        List<String> qlist = new ArrayList<String>();
        for (String string : list) {
            qlist.add("'" + string + "'");
        }
        return StringUtils.join(qlist, separator);
    }
}
