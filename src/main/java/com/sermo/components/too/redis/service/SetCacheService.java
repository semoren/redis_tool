package com.sermo.components.too.redis.service;

import java.util.Set;

/**
 * @author sermo
 * @version 2016年7月5日 
 */
public interface SetCacheService {
	
	public void add(String key, String...values);
	
	public boolean isMember(String key, String value);
	
	public void remove(String key, String...values);
	
	public Set<String> sMembers(String key);
	
}
