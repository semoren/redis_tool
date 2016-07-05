package com.sermo.components.too.redis.service;

import java.io.Serializable;

/**
 * @author sermo
 * @version 2016年7月5日 
 */
public interface DynamicCacheService {
	
	public <T> void set(final Class<T> type, final T value, final long expire);
	
	public <T> T get(final Class<T> type, final Serializable id);
	
	public void del(final Class<?> type, final Serializable id);
}
