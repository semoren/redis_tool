package com.sermo.components.too.redis.service.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.sermo.components.biz.exception.BaseException;
import com.sermo.components.biz.util.BaseUtil;
import com.sermo.components.biz.util.ExceptionUtil;
import com.sermo.components.too.redis.annotation.CacheEntity;
import com.sermo.components.too.redis.service.SerialService;
import com.sermo.components.too.redis.service.StaticCacheService;
import redis.clients.jedis.BuilderFactory;

/**
 * @author sermo
 * @version 2016年7月5日 
 */
@Service
public class StaticCacheServiceImpl implements StaticCacheService{

	private static Logger logger = LoggerFactory.getLogger(StaticCacheServiceImpl.class);
	
	private @Resource(name="redisStaticTemplate") RedisTemplate<Serializable, Object> redisTempate;
	
	private @Resource SerialService serialService;
	
	@Override
	public <T> void set(final Class<T> type, final T value) {
		redisTempate.execute(new RedisCallback<Object>() {
			
			@Override
			public Object doInRedis(RedisConnection connection) {
				try {
					CacheEntity entity = getCacheEntity(type);
					Object field = invokeMethod(type, value, entity.primary());
					if (field != null) {
						connection.hSet(serialService.serialize(entity.key()), 
								serialService.serialize(BaseUtil.STRING.parse(field)), 
								serialService.serialize(value));
					}
				} catch (BaseException e) {
					logger.error("Static cache service set error!", e);
				}
				return null;
			}
		}, true);
	}

	@Override
	public <T> void set(final Class<T> type, final Collection<T> values) {
		redisTempate.execute(new RedisCallback<Object>() {

			@Override
			public Object doInRedis(RedisConnection connection) {
				try {
					CacheEntity entity = getCacheEntity(type);
					Map<byte[], byte[]> map = new HashMap<>();
					for (T value : values) {
						Object field = invokeMethod(type, value, entity.primary());
						if (field != null) {
							map.put(serialService.serialize(field), serialService.serialize(value));
						}
					}
					connection.hMSet(entity.key().getBytes(), map);
				} catch (BaseException e) {
					logger.error("Static cache service set error!", e);
				}
				return null;
			}
			
		}, true);
	}

	@Override
	public <T> T get(final Class<T> type, final Serializable id) {
		return redisTempate.execute(new RedisCallback<T>() {

			@Override
			public T doInRedis(RedisConnection connection) {
				try {
					byte[] bytes = connection.hGet(serialService.serialize(getCacheEntityKey(type)), 
							serialService.serialize(id));
					return serialService.deserialize(type, bytes);
				} catch (BaseException e) {
					logger.error("Static cache service get error!", e);
				}
				return null;
			}
			
		}, true);
	}

	@Override
	public <T extends Comparable<T>> List<T> list(final Class<T> type) {
		return redisTempate.execute(new RedisCallback<List<T>>() {

			@Override
			public List<T> doInRedis(RedisConnection connection) {
				try {
					List<byte[]> bytes = connection.hVals(serialService.serialize(getCacheEntityKey(type)));
					List<T> list = new ArrayList<>(bytes.size());
					
					for (byte[] bs : bytes) {
						list.add(serialService.deserialize(type, bs));
					}
					Collections.sort(list);
					return list;
				} catch (BaseException e) {
					logger.error("Static cache service list error!", e);
				}
				return null;
			}
		}, true);
	}

	@Override
	public <T extends Comparable<T>> List<T> list(final Class<T> type, final Serializable... ids) {
		return redisTempate.execute(new RedisCallback<List<T>>() {

			@Override
			public List<T> doInRedis(RedisConnection connection) {
				try {
					byte[][] fields = new byte[ids.length][];
					for (int i = 0; i < ids.length; i++) {
						fields[i] = serialService.serialize(ids[i]);
					}
					List<byte[]> bytes = connection.hMGet(serialService.serialize(getCacheEntityKey(type)), fields);
					List<T> list = new ArrayList<>(bytes.size());
					
					for (byte[] bs : bytes) {
						list.add(serialService.deserialize(type, bs));
					}
					Collections.sort(list);
					return list;
				} catch (BaseException e) {
					logger.error("Static cache service list error", e);
				}
				return null;
			}
		}, true);
	}

	@Override
	public void del(final Class<?> type, final Serializable... ids) {
		redisTempate.execute(new RedisCallback<Object>() {

			@Override
			public Object doInRedis(RedisConnection connection) {
				try {
					byte[][] fields = new byte[ids.length][];
					
					for (int i = 0; i < ids.length; i++) {
						fields[i] = serialService.serialize(ids[i]);
					}
					connection.hDel(serialService.serialize(getCacheEntityKey(type)), fields);
				} catch (BaseException e) {
					logger.error("Static cache service del error!", e);
				}
				return null;
			}
		}, true);
	}

	@Override
	public void clear(final Class<?> type) {
		redisTempate.execute(new RedisCallback<Object>() {

			@Override
			public Object doInRedis(RedisConnection connection) {
				try {
					connection.del(serialService.serialize(getCacheEntityKey(type)));
				} catch (BaseException e) {
					logger.error("Static cache srvice clear error!", e);
				}
				return null;
			}
		}, true);
	}

	@Override
	public List<String> hmget(final String key, final String... codes) {
		return redisTempate.execute(new RedisCallback<List<String>>() {

			@Override
			public List<String> doInRedis(RedisConnection connection) {
				try {
					byte[] keys = serialService.serialize(key);
					byte[][] fields = new byte[codes.length][];
					
					for (int i = 0; i < codes.length; i++) {
						fields[i] = serialService.serialize(codes[i]);
					}
					if (connection.exists(keys)) {
						List<byte[]> values = connection.hMGet(keys, fields);
						return BuilderFactory.STRING_LIST.build(values);
					}
				} catch (BaseException e) {
					logger.error("Static cache service hmget error!", e);
				}
				return null;
			}
		}, true);
	}

	private CacheEntity getCacheEntity(Class<?> type) throws BaseException {
		if (!type.isAnnotationPresent(CacheEntity.class)) {
			throw ExceptionUtil.exception(503, "Class[#0] not annotation CacheEntity!", type);
		}
		return type.getAnnotation(CacheEntity.class);
	}
	
	private String getCacheEntityKey(Class<?> type) throws BaseException {
		return getCacheEntity(type).key();
	}
	
	private <T> Object invokeMethod(Class<T> type, T value, String method) throws BaseException {
		try {
			return type.getMethod(method).invoke(value);
		} catch (Exception e) {
			throw ExceptionUtil.exception(503, "invoke object[#0] method[#1] error!", e, type.getName(), method);
		}
	}
}
