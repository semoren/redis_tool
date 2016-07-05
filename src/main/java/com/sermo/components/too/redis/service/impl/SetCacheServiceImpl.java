package com.sermo.components.too.redis.service.impl;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.sermo.components.biz.exception.BaseException;
import com.sermo.components.too.redis.service.SerialService;
import com.sermo.components.too.redis.service.SetCacheService;

/**
 * @author sermo
 * @version 2016年7月5日 
 */
@Service
public class SetCacheServiceImpl implements SetCacheService{

	private static Logger logger = LoggerFactory.getLogger(SetCacheServiceImpl.class);
	
	private @Resource(name = "redisStaticTemplate") RedisTemplate<Serializable, Object> redisTemplate;
	
	private @Resource SerialService serialService; 
	
	@Override
	public void add(final String key, final String... values) {
		redisTemplate.execute(new RedisCallback<Object>() {

			@Override
			public Object doInRedis(RedisConnection connection){
				try {
					byte[] bkey = serialService.serialize(key);
					byte[][] bvalues = serialize(values);
					connection.sAdd(bkey, bvalues);
				} catch (BaseException e) {
					logger.error("set cache service add error!", e);
				}
				return null;
			}
			
		}, true);
	}

	@Override
	public boolean isMember(final String key, final String value) {
		return redisTemplate.execute(new RedisCallback<Boolean>() {

			@Override
			public Boolean doInRedis(RedisConnection connection){
				try {
					byte[] bkey = serialService.serialize(key);
					byte[] bvalue = serialService.serialize(value);
					return connection.sIsMember(bkey, bvalue);
				} catch (BaseException e) {
					logger.error("set cache service isMember error!", e);
				}
				return false;
			}
		}, true);
	}

	@Override
	public void remove(final String key, final String... values) {
		redisTemplate.execute(new RedisCallback<Object>() {

			@Override
			public Object doInRedis(RedisConnection connection){
				try {
					byte[] bkey = serialService.serialize(key);
					byte[][] bvalues = serialize(values);
					connection.sRem(bkey, bvalues);
				} catch (BaseException e) {
					logger.error("remove cache service sRem error!", e);
				}
				return null;
			}
			
		}, true);
	}

	@Override
	public Set<String> sMembers(final String key) {
		return redisTemplate.execute(new RedisCallback<Set<String>>() {

			@Override
			public Set<String> doInRedis(RedisConnection connection) {
				try {
					byte[] bkey = serialService.serialize(key);
					Set<byte[]> values = connection.sMembers(bkey);
					if (values != null && !values.isEmpty()) {
						return deserialize(values);
					}
				} catch (BaseException e) {
					logger.error("set cache service sMembers error", e);
				}
				return null;
			}
		}) ;
	}

	private byte[][] serialize(String...values) throws BaseException {
		
		byte[][] bvalues = new byte[values.length][];
		
		for (int i = 0; i < values.length; i++) {
			bvalues[i] = serialService.serialize(values[i]);
		}
		return bvalues;
	}
	
	private Set<String> deserialize(Set<byte[]> bytes) throws BaseException {
		
		Set<String> values = new HashSet<>(bytes.size());
		
		for (byte[] bts : bytes) {
			values.add(serialService.deserialize(String.class, bts));
		}
		return values;
	}
}
