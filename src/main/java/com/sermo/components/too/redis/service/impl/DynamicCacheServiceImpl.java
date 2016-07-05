package com.sermo.components.too.redis.service.impl;

import java.io.Serializable;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.sermo.components.biz.exception.BaseException;
import com.sermo.components.biz.util.ExceptionUtil;
import com.sermo.components.too.redis.annotation.CacheEntity;
import com.sermo.components.too.redis.service.DynamicCacheService;
import com.sermo.components.too.redis.service.SerialService;

/**
 * @author sermo
 * @version 2016年7月5日 
 */
@Service
public class DynamicCacheServiceImpl implements DynamicCacheService{

	private static Logger logger = LoggerFactory.getLogger(DynamicCacheServiceImpl.class);
	
	private @Resource(name = "redisDynamicTemplate") RedisTemplate<String, Object> redisTemplate;
	
	private @Resource SerialService serialService;
	
	@Override
	public <T> void set(final Class<T> type, final T value, final long expire) {
		redisTemplate.execute(new RedisCallback<Object>() {

			@Override
			public Object doInRedis(RedisConnection connection) {
				try {
					connection.setEx(serialService.serialize(getCacheKey(type, value)), expire, serialService.serialize(value));
				} catch (BaseException e) {
					logger.error("Dynamic cache service set error!", e);
				}
				return null;
			}
			
		}, true);
	}

	@Override
	public <T> T get(final Class<T> type, final Serializable id) {
		return redisTemplate.execute(new RedisCallback<T>() {

			@Override
			public T doInRedis(RedisConnection connection) {
				try {
					byte[] value = connection.get(serialService.serialize(getCachePrefix(type) + id));
					if (value == null || value.length == 0) {
						return null;
					}
					return serialService.deserialize(type, value);
				} catch (BaseException e) {
					logger.error("Dynamic cache service get error!", e);
					return null;
				}
			}
		}, true);
	}

	@Override
	public void del(final Class<?> type, final Serializable id) {
		redisTemplate.execute(new RedisCallback<Object>() {

			@Override
			public Object doInRedis(RedisConnection connection) {
				try {
					connection.del(serialService.serialize(getCachePrefix(type) + id));
				} catch (BaseException e) {
					logger.error("Dynamic cache service del error!", e);
				}
				return null;
			}
		}, true);
	}

	private <T> Object invokeMethod(Class<T> type, T value, String method) throws BaseException {
		try {
			return type.getMethod(method).invoke(value);
		} catch (Exception e) {
			throw ExceptionUtil.exception(503, "invoke object[#0] method[#1] error!", e, type.getName(), method);
		}
	}
	
	private String getCachePrefix(Class<?> type) throws BaseException {
		return getCacheEntity(type).key();
	}
	
	private <T> String getCacheKey(Class<T> type, T value) throws BaseException {
		CacheEntity entity = getCacheEntity(type);
		return entity.key() + invokeMethod(type, value, entity.primary());
	}
	
	private CacheEntity getCacheEntity(Class<?> type) throws BaseException {
		if (!type.isAnnotationPresent(CacheEntity.class)) {
			throw ExceptionUtil.exception(503, "Class[#0] not annotation CacheEntity!", type.getName());
		}
		return type.getAnnotation(CacheEntity.class);
	}
}
