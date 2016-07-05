package com.sermo.components.too.redis.service.impl;

import java.io.IOException;

import javax.annotation.Resource;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Service;

import com.sermo.components.biz.exception.BaseException;
import com.sermo.components.biz.util.ExceptionUtil;
import com.sermo.components.too.redis.service.SerialService;

/**
 * @author sermo
 * @version 2016年7月5日 
 */
@Service
public class SerialServiceImpl implements SerialService{

	private @Resource ObjectMapper mapper;
	
	@Override
	public byte[] serialize(Object value) throws BaseException {
		try {
			if (value instanceof String) {
				return ((String) value).getBytes();
			}
			return mapper.writeValueAsBytes(value);
		} catch (IOException e) {
			throw ExceptionUtil.exception(503, "JSON serialize[#0] error!", value);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T deserialize(Class<T> type, byte[] bytes) throws BaseException {
		try {
			if (String.class.equals(type)) {
				return (T) new String(bytes);
			}
			return mapper.readValue(bytes, type);
		} catch (IOException e) {
			throw ExceptionUtil.exception(503, "JSON deserialize type[#0] error", type);
		}
	}

}
