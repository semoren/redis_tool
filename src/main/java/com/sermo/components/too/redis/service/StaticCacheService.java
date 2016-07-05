package com.sermo.components.too.redis.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @author sermo
 * @version 2016年7月5日 
 */
public interface StaticCacheService {
	
	public <T> void set(final Class<T> type, final T value);
	
	public <T> void set(final Class<T> type, final Collection<T> values);
	
	public <T> T get(final Class<T> type, final Serializable id);
	
	public <T extends Comparable<T>> List<T> list(final Class<T> type);
	
	public <T extends Comparable<T>> List<T> list(final Class<T> type, final Serializable... ids);
	
	public void del(final Class<?> type, final Serializable...ids);
	
	public void clear(final Class<?> type);
	
	public List<String> hmget(final String key, final String...fields);
}
