package com.jianrc.frame.jpa;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 *
 * @author jianrc
 */
public final class ManagerFactory {

    private static final Map<String, EntityManagerFactory> cache = new HashMap<String, EntityManagerFactory>(1);

    public static final String DEFAULT_UNITNAME = "default-pu";

    private ManagerFactory() {
    }

    public static EntityManagerFactory getDefaultEntityManagerFactory() {
        return getEntityManagerFactory(DEFAULT_UNITNAME);
    }

    public static EntityManagerFactory getEntityManagerFactory(String name) {
        synchronized (ManagerFactory.class) {
            EntityManagerFactory factory = cache.get(name);
            if (factory == null || !factory.isOpen()) {
                factory = Persistence.createEntityManagerFactory(name);
                cache.put(name, factory);
            }
            return factory;
        }
    }

    /**
     *  使用PersistManager后使用setDefaultEntityManagerFactory就失效了
     *
     * @param factory
     */
    public static void setDefaultEntityManagerFactory(EntityManagerFactory factory) {
        setEntityManagerFactory(DEFAULT_UNITNAME, factory);
    }

    public static void setEntityManagerFactory(final String name, final EntityManagerFactory factory) {
        synchronized (ManagerFactory.class) {
            cache.put(name, factory);
        }
    }
}
