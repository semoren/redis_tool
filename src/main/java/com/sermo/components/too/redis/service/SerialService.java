package com.sermo.components.too.redis.service;

import com.sermo.components.biz.exception.BaseException;

/**
 * @author sermo
 * @version 2016年7月5日 
 */
public abstract interface SerialService {
	
	public abstract byte[] serialize(Object param) throws BaseException;
	
	public abstract <T> T deserialize(Class<T> paramClass, byte[] paramArrayOfByte) throws BaseException;
	
}
