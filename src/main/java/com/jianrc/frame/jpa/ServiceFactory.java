package com.jianrc.frame.jpa;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.jianrc.frame.api.Monitor;
import com.jianrc.frame.jpa.annotations.Transaction;

public class ServiceFactory {
	private static Logger LOG = LoggerFactory.getLogger(ServiceFactory.class);
	
	private static ThreadLocal<Map<String,EntityManager>> emThreadLocal = new ThreadLocal<Map<String,EntityManager>>();
		
	public static <T> T createProxyInstance(Class<T> ifClazz,
			Class<? extends T> implClazz){
		try{
			return (T) Proxy.newProxyInstance(ServiceFactory.class.getClassLoader(), new Class<?>[]{ifClazz}, new ServiceInvocationHandler(implClazz.newInstance()));
		}catch(Throwable e){
			throw new RuntimeException(e);
		}
	}
	
	public static class ServiceInvocationHandler implements InvocationHandler{
		private static Map<Method,Transaction> transactions = new HashMap<Method, Transaction>();
		
		private Object impl;

		public ServiceInvocationHandler(Object impl) {
			this.setImpl(impl);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			String className = method.getDeclaringClass().getName();
			String methodName = method.getName();
			
			//Monitor.addOne(in_+ logic_);
			//Monitor.addOne(in_+ logic_ + invoke_ + className + "." + methodName);
			
			long t1 = System.currentTimeMillis();
			try{
				//考虑嵌套调用时不需要重新打开关闭transaction和需要重用entitymanager
				Transaction t = getTransaction(method);
				if(t != null){
					String[] unitnames = t.unitnames();
					EntityManager[] ems = new EntityManager[unitnames.length];
					for (int i = 0; i < unitnames.length; i++) {
						//FIXME 考虑重用
						String name = unitnames[i];
						EntityManager em = createEntityManager(name);
						setThreadLocal(name, em);
						ems[i] = em;
					}

					boolean[] begins = new boolean[ems.length];
					for (int i = 0; i < ems.length; i++) {
						EntityManager em = ems[i];
						if(em.getTransaction().isActive()){
							begins[i] = false;
							if(LOG.isDebugEnabled()) LOG.debug("transaction not begin");
						}else{
							em.getTransaction().begin();
							begins[i] = true;
							if(LOG.isDebugEnabled()) LOG.debug("transaction begin:" + em);
						}
					}

					try{
						Object ret = method.invoke(impl, args);
						
						for (int i = 0; i < ems.length; i++) {
							EntityManager em = ems[i];
							em.flush();
							if(begins[i]) {
								if(LOG.isDebugEnabled()) LOG.debug("transaction commit:" + em);
								em.getTransaction().commit();
							}
						}
						
						//Monitor.addOne(succ_+ logic_ + invoke_);
						//Monitor.addOne(succ_+ logic_ + invoke_ + className + "." + methodName);
						return ret;
					}catch(InvocationTargetException e){
						for (int i = 0; i < ems.length; i++) {
							EntityManager em = ems[i];
							if(begins[i]) em.getTransaction().rollback();
						}
						
						//Monitor.addOne(err_+ logic_ + invoke_);
						//Monitor.addOne(err_+ logic_ + invoke_ + className + "." + methodName);
						throw e.getCause();
					}catch(Throwable e){
						for (int i = 0; i < ems.length; i++) {
							EntityManager em = ems[i];
							if(begins[i]) em.getTransaction().rollback();
						}
						
						//Monitor.addOne(fail_+ logic_);
						//Monitor.addOne(fail_+ logic_ + className + "." + methodName);
						throw e;
					}finally{
						for (int i = 0; i < ems.length; i++) {
							EntityManager em = ems[i];
							//if(begins[i]) em.close();
							//FIXME 考虑重用
							em.close();
						}
						
						emThreadLocal.remove();
					}
				}else{
					try{
						return method.invoke(impl, args);
					}catch(InvocationTargetException e){
						throw e.getCause();
					}catch(Throwable e){
						throw e;
					}
				}
			}finally{
				long t2 = System.currentTimeMillis();
				long timeuse = t2-t1;
				
				if(timeuse > 100) {
					//Monitor.addOne(time_ + logic_ + gt100_);
					//Monitor.addOne(time_ + logic_ + gt100_ + className + "." + methodName);
				}
				if(timeuse > 500) {
					//Monitor.addOne(time_ + logic_ + gt500_);
					//Monitor.addOne(time_ + logic_ + gt500_ + className + "." + methodName);
				}
				if(timeuse > 2000){
					//Monitor.addOne(time_ + logic_ + gt2000_);
					//Monitor.addOne(time_ + logic_ + gt2000_ + className + "." + methodName);
				}
				if(timeuse > 4000){
					//Monitor.addOne(time_ + logic_ + gt4000_);
					//Monitor.addOne(time_ + logic_ + gt4000_ + className + "." + methodName);
				}
				if(timeuse > 8000){
					//Monitor.addOne(time_ + logic_ + gt8000_);
					//Monitor.addOne(time_ + logic_ + gt8000_ + className + "." + methodName);
				}
				if(timeuse > 16000){
					//Monitor.addOne(time_ + logic_ + gt16000_);
					//Monitor.addOne(time_ + logic_ + gt16000_ + className + "." + methodName);
				}
				
				if(LOG.isDebugEnabled()) {
					LOG.debug("call " + className + "." + methodName + " time used : " + timeuse);
				}
			}
			
		}

		private void setThreadLocal(String name, EntityManager em) {
			Map<String, EntityManager> trans = emThreadLocal.get();
			if(trans == null) {
				trans = new HashMap<String, EntityManager>(1);
				emThreadLocal.set(trans);
			}
			trans.put(name, em);
		}

		private static Transaction getTransaction(Method method){
			Transaction t = transactions.get(method);
			if(t == null && !transactions.containsKey(method)){
				t = getMethodOrClassAnnotation(method, Transaction.class);
				transactions.put(method, t);
			}
			return t;
		}
		
		public Object getImpl() {
			return impl;
		}

		public void setImpl(Object impl) {
			this.impl = impl;
		}
	}
	
	public static EntityManager getEntityManager(){
		return getEntityManager("default-pu");
	}
	
	public static EntityManager getEntityManager(String name){
		Map<String, EntityManager> emmap = emThreadLocal.get();
		if(emmap == null || !emmap.containsKey(name)){
			return createEntityManager(name);
		}else{
			return emmap.get(name);
		}
	}

	private static EntityManager createEntityManager(String name) {
		return ManagerFactory.getEntityManagerFactory(name).createEntityManager();
	}
	

	public static <T extends Annotation> T getMethodOrClassAnnotation(Method method, Class<T> annoClazz) {
		T mthAnno = method.getAnnotation(annoClazz);
        if(mthAnno != null) return mthAnno;
        
        Class<?> declareClazz = method.getDeclaringClass();
        
		T clzAnno = declareClazz.getAnnotation(annoClazz);
        if(clzAnno != null) return clzAnno;
        
        Class<?>[] interfaces = declareClazz.getInterfaces();
        for (Class<?> i : interfaces) {
            T ann = i.getAnnotation(annoClazz);
            if(ann != null) return ann;
        }
        
        for (Class<?> i : interfaces) {
            Method[] methods = i.getMethods();
			for (Method m : methods) {
                if(m.getName().equals(method.getName())){
                    T ann = m.getAnnotation(annoClazz);
                    if(ann != null) return ann;
                }
            }
        }
        
        return null;
	}
}
