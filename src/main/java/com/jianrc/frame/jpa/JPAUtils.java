package com.jianrc.frame.jpa;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;

import com.jianrc.frame.jpa.bean.Fliper;
import com.jianrc.frame.jpa.bean.Sheet;

public class JPAUtils {

    public static String genWhereJPQL(final Class<?> entityClass, final Object bean) {
        return genWhereJPQL(genTableAlias(entityClass), bean);
    }
    
    /**
     * @deprecated
     */
    public static String genWhereJPQL(final String alias, final Object bean) {
        if (bean == null) return "";
        final Method[] methods = loadOptionGetter(bean.getClass());
        if (methods.length < 1) return "";
        SiftColumn getter = null;
        Object value = null;
        boolean ranged = false;
        String prefix = "WHERE ";
        StringBuilder result = new StringBuilder();
        try {
            for (Method method : methods) {
                getter = method.getAnnotation(SiftColumn.class);
                if (getter.gensql()) {
                    value = method.invoke(bean, alias);
                    if (value == null || value.toString().isEmpty()) continue;
                    result.append(prefix).append(value);
                } else {
                    value = method.invoke(bean);
                    if (CharSequence.class.isAssignableFrom(method.getReturnType()) || value instanceof CharSequence) {
                        CharSequence cs = (CharSequence)value;
						if (cs == null || cs.length() == 0) continue;
                    } else if (value instanceof BigInteger) {
                        if (((BigInteger) value).compareTo(BigInteger.valueOf(getter.least())) < 0) continue;
                    } else if (value instanceof Number) {
                        if (((Number) value).longValue() < getter.least()) continue;
                    }
                    int pos = getter.jpaname().indexOf("{alias}");
                    result.append(prefix).append(pos >= 0 ? getter.jpaname().replace((CharSequence) "{alias}", alias) : (getter.jpaname().indexOf('.') > 0 ? getter.jpaname() : (alias + '.' + getter.jpaname()))).append(' ').append(getter.ship().sign()).append(' ');
                    if (getter.paramidx() > 0) {
                        result.append('?').append(getter.paramidx());
                    } else {
                        ranged = (getter.ship() == ColumnShip.IN || getter.ship() == ColumnShip.NOTIN);
                        result.append(ranged ? '(' : '\'').append(value).append(ranged ? ')' : '\'');
                    }
                }
                prefix = " AND ";
            }
        } catch (Exception ex) {
            throw new RuntimeException(alias + "; " + bean, ex);
        }
        return result.toString();
    }
    
    public static Number getSingleNumberResult(EntityManager em,final SQLType type, final String sql, final Object... params) {
        Object obj = getSingleResult(em,type, sql, params);
        return (obj instanceof List) ? ((List<Number>) obj).get(0) : (Number) obj;
    }

    public static <T> T getSingleResult(EntityManager em,final SQLType type, final String sql, final Object... params) {
        Query query = createQuery(em, type, sql);
        if(type == SQLType.JPQL) query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        setQueryParameters(query, params);
        return (T) query.getSingleResult();
    }

    
    public static int[] executeUpdate(EntityManager em,final SQLType type, final SQLAndParams... sqlAndParams) {
        int[] results = new int[sqlAndParams.length];
        int i = -1;
        for (final SQLAndParams sqlParams : sqlAndParams) {
            Query query = createQuery(em,type,sqlParams.getSql());
            if(sqlParams.getParams() != null) setQueryParameters(query,sqlParams.getParams().toArray());
            results[++i] = query.executeUpdate();
        }
        return results;
    }
    
    public static int[] executeUpdates(EntityManager em,final SQLType type, final String... sqls) {
        int[] results = new int[sqls.length];
        int i = -1;
        for (final String sql : sqls) {
            results[++i] = createQuery(em, type, sql).executeUpdate();
        }
        return results;
    }
    
    public static int executeJPQL(EntityManager em,final String sql, final Object... params) {
    	return executeUpdate(em,SQLType.JPQL,sql,params);
    }
    
    public static int executeUpdate(EntityManager em,final SQLType type, final String sql, final Object... params) {
        Query query = createQuery(em, type, sql);
        setQueryParameters(query, params);
        return query.executeUpdate();
    }
    
    private static final Map<Class<?>, Method[]> optionGetterCache = new HashMap<Class<?>, Method[]>();
    private static final Comparator<Object[]> gettercomparator = new Comparator<Object[]>() {
        @Override
        public int compare(Object[] o1, Object[] o2) {
            return (Integer) o1[0] - (Integer) o2[0];
        }
    };
    
    private static Method[] loadOptionGetter(final Class<?> clazz) {
        Method[] result = optionGetterCache.get(clazz);
        if (result == null) {
            synchronized (optionGetterCache) {
                result = optionGetterCache.get(clazz);
                if (result == null) {
                    SiftColumn getter = null;
                    List<Object[]> list = new ArrayList<Object[]>();
                    Class<?> tmpclz = clazz;
                    try {
                        List<String> set = new ArrayList<String>();
                        do {
                            for (Method method : tmpclz.getMethods()) {
                                getter = method.getAnnotation(SiftColumn.class);
                                if (getter == null) continue;
                                if (set.indexOf(method.getName()) > -1) continue;
                                if (getter.gensql() && method.getParameterTypes().length != 1) continue;
                                list.add(new Object[]{getter.paramidx(), method});
                                set.add(method.getName());
                            }
                        } while ((tmpclz = tmpclz.getSuperclass()) != Object.class);
                    } catch (Exception ex) {
                        throw new RuntimeException("class = " + tmpclz, ex);
                    }
                    Collections.sort(list, gettercomparator);

                    result = new Method[list.size()];
                    for (int i = 0; i < result.length; i++) {
                        result[i] = (Method) list.get(i)[1];
                    }
                    optionGetterCache.put(clazz, result);
                }
            }
        }
        return result;
    }
    
    private static SQLAndParams genWhereJPQLNew(final String alias, final Object bean) {
        if (bean == null) return new SQLAndParams("", new ArrayList<Object>(0));
        final Method[] methods = loadOptionGetter(bean.getClass());
        if (methods.length < 1) return new SQLAndParams("", new ArrayList<Object>(0));
        
        SiftColumn getter = null;
        Object value = null;
        boolean ranged = false;
        String prefix = "WHERE ";
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<Object>();
        int idx = 1;
        try {
            for (Method method : methods) {
                getter = method.getAnnotation(SiftColumn.class);
                if (getter.gensql()) {
                    value = method.invoke(bean, alias);
                    if (value == null || value.toString().isEmpty()) continue;
                    sql.append(prefix).append(value);
                } else if(getter.ship() == ColumnShip.KEYWORD){
                	value = method.invoke(bean);
                	if(value==null) continue;
                	String[] names = getter.keywordNames();
                	if(names == null || names.length == 0) continue;
                	sql.append(prefix);
                	
                	StringBuffer condition = new StringBuffer();
                	condition.append("(");
                	for (String name : names) {
                        int pos = name.indexOf("{alias}");
                        condition.append(pos >= 0 ? name.replace((CharSequence) "{alias}", alias) : (name.indexOf('.') > 0 ? name : (alias + '.' + name))).append(' ').append(getter.ship().sign()).append(' ');
                        condition.append("?").append(idx);
                        condition.append(" OR ");
                        idx ++;
					}
                	condition.delete(condition.length() - 3, condition.length());
                	condition.append(")");
                	
                	sql.append(condition);
                	for (String name : names) {
                		params.add("%" + value + "%");
                	}
                } else {
                    value = method.invoke(bean);
                    if(value==null) continue;
                    if (CharSequence.class.isAssignableFrom(method.getReturnType()) || value instanceof CharSequence) {
                        CharSequence cs = (CharSequence)value;
						if (cs == null || cs.length() == 0) continue;
                    } else if (value instanceof BigInteger) {
                        if (((BigInteger) value).compareTo(BigInteger.valueOf(getter.least())) < 0) continue;
                    } else if (value instanceof Number) {
                        if (((Number) value).longValue() < getter.least()) continue;
                    }
                    int pos = getter.jpaname().indexOf("{alias}");
                    sql.append(prefix).append(pos >= 0 ? getter.jpaname().replace((CharSequence) "{alias}", alias) : (getter.jpaname().indexOf('.') > 0 ? getter.jpaname() : (alias + '.' + getter.jpaname()))).append(' ').append(getter.ship().sign()).append(' ');
                    
                    ranged = (getter.ship() == ColumnShip.IN || getter.ship() == ColumnShip.NOTIN);
                    if(ranged){
                        //sql.append("(?)").append(idx);
                        sql.append('?').append(idx);
                    } else {
                        sql.append('?').append(idx);
                    }
                    
                    if(getter.ship() == ColumnShip.LIKE || getter.ship() == ColumnShip.NOTLIKE){
                        params.add("%" + value + "%");
                    }else{
                        params.add(value);
                    }
                    idx ++;
                }
                prefix = " AND ";
            }
        } catch (Exception ex) {
            throw new RuntimeException(alias + "; " + bean, ex);
        }
        return new SQLAndParams(sql.toString(), params);
    }

    public static long getCount(EntityManager em,final Object bean, final Class<?> entityClass, final Object... params) {
        String alias = genTableAlias(entityClass);
        String countsql = "SELECT COUNT(" + alias + ") FROM " + entityClass.getSimpleName() + " " + alias + " " + genWhereJPQL(entityClass, bean);
        return getSingleNumberResult(em,SQLType.JPQL, countsql, params).longValue();
    }

    public static <T> List<T> queryList(final Object bean, final Class<T> entityClass, final Object... params) {
        return queryList(bean, entityClass, (Fliper) null, params);
    }

    public static <T> List<T> queryList(final Object bean, final Class<T> entityClass, final Fliper fliper, final Object... params) {
        return queryList(entityClass, genTableAlias(entityClass), genWhereJPQL(entityClass, bean), fliper, params);
    }

    public static <T> List<T> queryList(EntityManager em,final Class<T> entityClass, final String alias, String wheresql, Fliper fliper, final Object... params) {
        String sql = "SELECT " + alias + " FROM " + entityClass.getSimpleName() + " " + alias + " " + wheresql;
        if (fliper != null && fliper.isNotEmptySortColumn()) {
        	sql += " ORDER BY " + alias + "." + fliper.getSortColumn();
        } else if (entityClass.getAnnotation(SiftOrderBy.class) != null) {
            String orderby = entityClass.getAnnotation(SiftOrderBy.class).value();
            sql += " ORDER BY " + orderby.replace((CharSequence) "{alias}", alias);
        }
        return queryList(em,SQLType.JPQL, sql, fliper, params);
    }

    public static <T> Sheet<T> querySheet(EntityManager em,final Object bean, final Class<T> entityClass, final Object... params) {
        return querySheet(em,bean, entityClass, (Fliper) null, params);
    }

    public static long count(EntityManager em,final Object bean, final Class entityClass) {
        String alias = genTableAlias(entityClass);
        SQLAndParams sqlAndParams = genWhereJPQLNew(alias, bean);
        
        String countsql = "SELECT COUNT(" + alias + ") FROM " + entityClass.getSimpleName() + " " + alias + " " + sqlAndParams.getSql();
        long rowcount = getSingleNumberResult(em,SQLType.JPQL, countsql, sqlAndParams.getParams().toArray()).longValue();
        return rowcount;
    }
    
    public static <T> Sheet<T> querySheetNew(EntityManager em,final Object bean, final Class<T> entityClass, final Fliper fliper) {
        String alias = genTableAlias(entityClass);
        SQLAndParams sqlAndParams = genWhereJPQLNew(alias, bean);
        return querySheet(em,entityClass, alias, sqlAndParams.getSql(), fliper, sqlAndParams.getParams().toArray());
    }
    
    public static <T> Sheet<T> querySheet(EntityManager em,final Object bean, final Class<T> entityClass, final Fliper fliper, final Object... params) {
        return querySheet(em,entityClass, genTableAlias(entityClass), genWhereJPQL(entityClass, bean), fliper, params);
    }

    public static <T> Sheet<T> querySheet(EntityManager em,final Class<T> entityClass, final String alias, String wheresql, final Object... params) {
        return querySheet(em,entityClass, alias, wheresql, (Fliper) null, params);
    }

    public static <T> Sheet<T> querySheet(EntityManager em,final Class<T> entityClass, final String alias, final String wheresql, Fliper fliper, final Object... params) {
        String countsql = "SELECT COUNT(" + alias + ") FROM " + entityClass.getSimpleName() + " " + alias + " " + wheresql;
        long rowcount = getSingleNumberResult(em,SQLType.JPQL, countsql, params).longValue();
        if(fliper != null){
            fliper.setRowcount((int) rowcount);
        }
        
        if (rowcount == 0) return Sheet.EMPTY;
        String sql = "SELECT " + alias + " FROM " + entityClass.getSimpleName() + " " + alias + " " + wheresql;
        if (fliper != null && fliper.isNotEmptySortColumn()) {
            if (fliper.getSortColumn().indexOf("{alias}") >= 0) {
                sql += " ORDER BY " + fliper.getSortColumn().replace((CharSequence) "{alias}", alias);
            } else {
                sql += " ORDER BY " + alias + "." + fliper.getSortColumn();
            }
        } else if (entityClass.getAnnotation(SiftOrderBy.class) != null) {
            String orderby = entityClass.getAnnotation(SiftOrderBy.class).value();
            sql += " ORDER BY " + orderby.replace((CharSequence) "{alias}", alias);
        }
        List<T> list = queryList(em,SQLType.JPQL, sql, fliper, params);
        return new Sheet<T>(rowcount, list);
    }

    public static <T> List<T> queryList(final Class<T> entityClass, final Object... params) {
        return queryList(entityClass, 0, params);
    }

    public static <T> List<T> queryList(EntityManager em,final Class<T> entityClass, final int index, final Object... params) {
        QuerySift field = parseQueryKey(entityClass, index);
        QuerySiftColumn[] names = (field == null) ? new QuerySiftColumn[0] : field.value();
        int length = (params == null) ? 0 : params.length;
        if (names.length < length) length = names.length;
        final String alias = genTableAlias(entityClass);
        final String protasis = (length == 0) ? "" : genProtasisString(alias, field, params);
        StringBuilder querystring = new StringBuilder(128);
        querystring.append("SELECT ").append(alias).append(" FROM ").append(entityClass.getSimpleName());
        querystring.append(" ").append(alias).append(" ").append(protasis);
        if (field != null && !field.orderby().isEmpty()) {
            querystring.append(" ORDER BY ").append(field.orderby().replace((CharSequence) "{alias}", alias));
        }
        Query query = em.createQuery(querystring.toString());
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        Range range = null;
        for (int i = 0; i < length; i++) {
            if (params[i] == null) continue;
            if (params[i] instanceof Range) {
                range = (Range) params[i];
                query.setParameter(names[i].value() + "_1", range.getMin());
                query.setParameter(names[i].value() + "_2", range.getMax());
            } else {
                query.setParameter(names[i].value(), params[i]);
            }
        }
        return query.getResultList();
    }

    public static <T> List<T> queryList(EntityManager em,final SQLType type, final String sql, final Object... params) {
        return queryList(em,type, sql, (Fliper) null, params);
    }

    public static <T> List<T> queryList(EntityManager em,final SQLType type, final String sql, final Fliper fliper, final Object... params) {
        Query query = createQuery(em, type, sql);
        if(type == SQLType.JPQL) query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        
        setQueryParameters(query, params);
        if (fliper != null && fliper.getPageSize() > 0) {
            query.setFirstResult(fliper.getFirstIndex());
            query.setMaxResults(fliper.getPageSize());
        }
        return (List<T>) query.getResultList();
    }

    public static <T> List<T> queryListLimit(EntityManager em,final SQLType type, final String sql, final int limit, final Object... params) {
        Query query = createQuery(em, type, sql);
        if(type == SQLType.JPQL) query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        
        setQueryParameters(query, params);
        query.setMaxResults(limit);
        return (List<T>) query.getResultList();
    }
    
    public static <T> Sheet<T> querySheet(EntityManager em,final SQLType type, final String countsql,final String datasql, final Fliper fliper, final Object... params) {
        Number count = find(em,type, countsql, params);
        long iCount = count.longValue();
        if(fliper != null){
            fliper.setRowcount((int) iCount);
        }
        List<T> data = null;
        if(iCount <= 0){
            data = Collections.EMPTY_LIST;
        }else{
            data = queryList(em,type, datasql,fliper,params);
        }
        return new Sheet<T>(iCount,data);
    }
    
    public static <T> List<T> queryListByNativedSQL(EntityManager em,final Class<T> entityClass, final String sql) {
        return queryListByNativedSQL(em,entityClass, sql, null);
    }

    public static <T> List<T> queryListByNativedSQL(EntityManager em,final Class<T> entityClass, final String sql, final Fliper fliper, final Object... params) {
        Query query = em.createNativeQuery(sql, entityClass);
        setQueryParameters(query, params);
        if (fliper != null && fliper.getPageSize() > 0) {
            query.setFirstResult(fliper.getFirstIndex());
            query.setMaxResults(fliper.getPageSize());
        }
        return (List<T>) query.getResultList();
    }

    public static <T> List<T> queryListByNativedSQL(EntityManager em,final String resultmapping, final String sql, final Fliper fliper, final Object... params) {
        Query query = em.createNativeQuery(sql, resultmapping);
        setQueryParameters(query, params);
        if (fliper != null && fliper.getPageSize() > 0) {
            query.setFirstResult(fliper.getFirstIndex());
            query.setMaxResults(fliper.getPageSize());
        }
        return (List<T>) query.getResultList();
    }

    public static <T> Sheet<T> querySheet(EntityManager em,Class<T> entityClass, Object... params) {
        return querySheet(em,entityClass, (Fliper) null, params);
    }

    public static <T> Sheet<T> querySheet(EntityManager em,final Class<T> entityClass, final Fliper fliper, final Object... params) {
        return querySheet(em,entityClass, 0, fliper, params);
    }

    public static <T> Sheet<T> querySheet(EntityManager em,final Class<T> entityClass, final int index, final Object... params) {
        return querySheet(em,entityClass, index, (Fliper) null, params);
    }

    public static <T> Sheet<T> querySheet(EntityManager em,final Class<T> entityClass, final int index, final Fliper fliper, final Object... params) {
        QuerySift field = parseQueryKey(entityClass, index);
        QuerySiftColumn[] names = (field == null) ? null : field.value();
        int length = params.length;
        final String alias = genTableAlias(entityClass);
        final String protasis = (length == 0) ? "" : genProtasisString(alias, field, params);
        
        String qry = "SELECT COUNT(" + alias + ") FROM " + entityClass.getSimpleName() + " " + alias + " " + protasis;
        Query query = em.createQuery(qry);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        Range range = null;
        for (int i = 0; i < length; i++) {
            if (params[i] == null) continue;
            if (params[i] instanceof Range) {
                range = (Range) params[i];
                query.setParameter(names[i].value() + "_1", range.getMin());
                query.setParameter(names[i].value() + "_2", range.getMax());
            } else {
                query.setParameter(names[i].value(), params[i]);
            }
        }
        final long rowcount = ((Number) query.getSingleResult()).longValue();
        if (rowcount < 1) return (Sheet<T>) Sheet.EMPTY;
        StringBuilder querystring = new StringBuilder(128);
        querystring.append("SELECT ").append(alias).append(" FROM ").append(entityClass.getSimpleName());
        querystring.append(" ").append(alias).append(" ").append(protasis);
        if (fliper != null && fliper.isNotEmptySortColumn()) {
            if(fliper.getSortColumn().startsWith(alias + ".")){
                querystring.append(" ORDER BY ").append(fliper.getSortColumn());
            }else{
                querystring.append(" ORDER BY ").append(alias + ".").append(fliper.getSortColumn());
            }
        } else if (field != null && !field.orderby().isEmpty()) {
            querystring.append(" ORDER BY ").append(field.orderby().replace((CharSequence) "{alias}", alias));
        }
        query = em.createQuery(querystring.toString());
        for (int i = 0; i < length; i++) {
            if (params[i] == null) continue;
            if (params[i] instanceof Range) {
                range = (Range) params[i];
                query.setParameter(names[i].value() + "_1", range.getMin());
                query.setParameter(names[i].value() + "_2", range.getMax());
            } else {
                query.setParameter(names[i].value(), params[i]);
            }
        }
        if (fliper != null && fliper.getPageSize() > 0) {
            query.setFirstResult(fliper.getFirstIndex());
            query.setMaxResults(fliper.getPageSize());
        }
        return new Sheet<T>(rowcount, query.getResultList());
    }

    private static String genProtasisString(final String alias, final QuerySift field, final Object... params) {
        final QuerySiftColumn[] names = field.value();
        final String u = field.and() ? " AND " : " OR ";
        StringBuilder protasis = new StringBuilder(64);
        int length = params.length;
        if (names.length < length) length = names.length;
        boolean first = true;
        boolean inside = true;
        String sp = "=";
        for (int i = 0; i < length; i++) {
            if (params[i] == null) continue;
            protasis.append(first ? " WHERE " : u).append(" ");
            sp = names[i].ship().sign();
            if (params[i] instanceof Range) {
                Range range = (Range) params[i];
                inside = range.isInside();
                protasis.append("(").append(alias).append(".").append(names[i].value());
                protasis.append(inside ? " >= :" : " < :").append(names[i].value()).append("_1");
                protasis.append(inside ? " AND " : " OR ").append(alias).append(".").append(names[i].value());
                protasis.append(inside ? " <= :" : " > :").append(names[i].value()).append("_2 )");
            } else {
                protasis.append(alias).append(".").append(names[i].value());
                protasis.append(" ").append(sp).append(" :").append(names[i].value());
            }
            if (first) first = false;
        }
        return protasis.toString();
    }

    private static String genKeyProtasisString(final String alias, final String[] names, final Object... params) {
        StringBuilder protasis = new StringBuilder(64);
        int length = params.length;
        if (names.length < length) length = names.length;
        boolean first = true;
        final String u = " AND ";
        for (int i = 0; i < length; i++) {
            if (params[i] == null) continue;
            protasis.append(first ? " WHERE " : u).append(" ").append(alias).append(".").append(names[i]);
            protasis.append(" = :").append(names[i]);
            if (first) first = false;
        }
        return protasis.toString();
    }

    private static Query createQuery(final EntityManager manager, final SQLType type, final String sql) {
        if (type == SQLType.NAMED) return manager.createNamedQuery(sql);
        if (type == SQLType.NATIVE) return manager.createNativeQuery(sql);
        Query query = manager.createQuery(sql);
        return query;
    }

    private static QuerySift parseQueryKey(final Class<?> entityClass, final int index) {
        QuerySifts cc = entityClass.getAnnotation(QuerySifts.class);
        if (cc == null) return null;//throw new IllegalArgumentException(entityClass + " have no QuerySifts Annotation ");
        return cc.value()[index];
    }

    private static UniqueSift parseIdKey(final Class<?> entityClass, final int index) {
        UniqueSifts cc = entityClass.getAnnotation(UniqueSifts.class);
        if (cc == null) return null;//throw new IllegalArgumentException(entityClass + " have no QuerySifts Annotation ");
        return cc.value()[index];
    }

    public static String genTableAlias(final Class<?> entityClass) {
        return "alias_" + entityClass.getSimpleName().toLowerCase();
    }

    public static <T> T findByIdKey(EntityManager em,final Class<T> entityClass, final Object... pks) {
        return findByIdKey(em,entityClass, 0, pks);
    }

    public static <T> T findByIdKey(EntityManager em,final Class<T> entityClass, final int index, final Object... pks) {
        final UniqueSift field = parseIdKey(entityClass, index);
        final String alias = genTableAlias(entityClass);
        String[] names = field.value();
        StringBuilder querystring = new StringBuilder(128);
        querystring.append("SELECT OBJECT(").append(alias).append(") FROM ").append(entityClass.getSimpleName());
        querystring.append(" ").append(alias).append(" ");
        querystring.append(genKeyProtasisString(alias, names, pks));
        final int length = pks.length;
        Query query = em.createQuery(querystring.toString());
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        for (int i = 0; i < length; i++) {
            if (pks[i] == null) continue;
            query.setParameter(names[i], pks[i]);
        }
        query.setMaxResults(1);
        List<T> list = query.getResultList();
//        try{
//	        for (T o : list) {
//				//if(o != null) em.refresh(o);
//			}
//        }catch(Throwable e){}
        return list.isEmpty() ? null : list.get(0);
    }

    public static <T> T findByQuery(EntityManager em,final String sql, final Object... params){
    	return find(em, SQLType.JPQL, sql, params);
    }
    
    public static <T> T find(EntityManager em,final SQLType type, final String sql, final Object... params) {
        Query query = createQuery(em, type, sql);
        if(type == SQLType.JPQL) query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        
        setQueryParameters(query, params);
        query.setMaxResults(1);
        List<T> list = (List<T>) query.getResultList();
        
//        try{
//        	//for (T o : list) {
//				//if(o != null) em.refresh(o);
//        	//}
//        }catch(Throwable e){
//        	e.printStackTrace();
//        }
        return list.isEmpty() ? null : list.get(0);
    }

    public static <T> T find(EntityManager em,Class<T> entityClass, Serializable pk) {
        T o = em.find(entityClass, pk);
        //try{
        	//if(o != null) em.refresh(o);
        //}catch(Throwable e){}
        return o;
    }

    public static <T extends Serializable> void insert(EntityManager em,T... entitys) {
        for (final Object entity : entitys) {
            if (entity.getClass().isArray()) {
                for (Object inneentity : (Object[]) entity) {
                	em.persist(inneentity);
                	em.flush();
                }
            } else {
            	em.persist(entity);
            }
        }
    }

    public static <T extends Serializable> void insert(EntityManager em,Collection<T> entitys) {
        for (final Object entity : entitys) {
            em.persist(entity);
        }
    }

    public static <T extends Serializable> void update(EntityManager em,T... entitys) {
        for (final Object entity : entitys) {
            if (entity.getClass().isArray()) {
                for (Object inneentity : (Object[]) entity) {
                    em.merge(inneentity);
                }
            } else {
                em.merge(entity);
            }
        }
    }

    public static <T extends Serializable> void update(EntityManager em,Collection<T> entitys) {
        for (final Object entity : entitys) {
            em.merge(entity);
        }
    }

    public static <T, E extends Serializable> void delete(EntityManager em,Class<T> clazz, Collection<E> pks) {
        Object entity = null;
        for (final Object pk : pks) {
            entity = em.find(clazz, pk);
            if (entity != null) {
                em.remove(entity);
            }
        }
    }

    public static void delete(EntityManager em,Object obj) {
        obj = em.merge(obj);
        em.remove(obj);
    }
    
    public static <T, E extends Serializable> void delete(EntityManager em,Class<T> clazz, E... pks) {
        Object entity = null;
        for (final Object pk : pks) {
            if (pk.getClass().isArray()) {
                int len = Array.getLength(pk);
                for (int i = 0; i < len; i++) {
                    entity = em.find(clazz, Array.get(pk, i));
                    if (entity != null) {
                    	em.remove(entity);
                    }
                }
            } else {
                entity = em.find(clazz, pk);
                if (entity != null) {
                	em.remove(entity);
                }
            }
        }
    }
    
    private static void setQueryParameters(Query query, final Object... params) {
        int i = 0;
        for (Object param : params) {
            if(param instanceof Map){
                Map<Object,Object> map = (Map<Object,Object>) param;
                for (Entry entry : map.entrySet()) {
                    String name = entry.getKey().toString();
                    Object value = entry.getValue();
                    query.setParameter(name, value);
                }
            }else{
                //if(param == null) continue;
            	if(param != null && param.getClass().isArray()){
            		Class<?> componentType = param.getClass().getComponentType();
					if(componentType.isPrimitive()){
            			if(componentType.equals(Boolean.TYPE)){
            				param = ArrayUtils.toObject((boolean[])param);
            			}else if(componentType.equals(Character.TYPE)){
            				param = ArrayUtils.toObject((char[])param);
            			}else if(componentType.equals(Short.TYPE)){
            				param = ArrayUtils.toObject((short[])param);
            			}else if(componentType.equals(Byte.TYPE)){
            				param = ArrayUtils.toObject((byte[])param);
            			}else if(componentType.equals(Integer.TYPE)){
            				param = ArrayUtils.toObject((int[])param);
            			}else if(componentType.equals(Long.TYPE)){
            				param = ArrayUtils.toObject((long[])param);
            			}else if(componentType.equals(Float.TYPE)){
            				param = ArrayUtils.toObject((float[])param);
            			}else if(componentType.equals(Double.TYPE)){
            				param = ArrayUtils.toObject((double[])param);
            			}
            		}
            		param = Arrays.asList((Object[]) param);
            	}
                query.setParameter(i + 1, param);
            }
            i++;
        }
    }

    public static class SQLAndParams{
        private String sql;
        private List<Object> params;
        public SQLAndParams(String sql, Object... params) {
            this.sql = sql;
            List<Object> list = new ArrayList<Object>(params.length);
            for (Object object : params) {
                list.add(object);
            }
            this.params = list;
        }
        
        public SQLAndParams(String sql, List<Object> params) {
            this.sql = sql;
            this.params = params;
        }
        public String getSql() {
            return sql;
        }
        public void setSql(String sql) {
            this.sql = sql;
        }
        public List<Object> getParams() {
            return params;
        }
        public void setParams(List<Object> params) {
            this.params = params;
        }
    }
}
