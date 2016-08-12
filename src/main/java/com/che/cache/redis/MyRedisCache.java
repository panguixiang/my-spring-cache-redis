package com.che.cache.redis;

import java.util.Arrays;
import java.util.Set;

import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 重写 RedisCache 加入 过期失效参数（put方法，其他不动）
 * @author panguixiang
 *
 */
public class MyRedisCache implements Cache{

	private static final int PAGE_SIZE = 128;
	private final String name;
	@SuppressWarnings("rawtypes") 
	private final RedisTemplate redisTemplate;
	private final byte[] prefix;
	private final byte[] setName;
	private final byte[] cacheLockName;
	private long WAIT_FOR_LOCK = 300;

	/**
	 * Constructs a new <code>RedisCache</code> instance.
	 * 
	 * @param name cache name
	 * @param prefix
	 * @param template
	 * @param expiration
	 */
	public MyRedisCache(String name, byte[] prefix, RedisTemplate<? extends Object, ? extends Object> template) {

		this.name = name;
		this.redisTemplate = template;
		this.prefix = prefix;
		StringRedisSerializer stringSerializer = new StringRedisSerializer();
		this.setName = stringSerializer.serialize(name + "~keys");
		this.cacheLockName = stringSerializer.serialize(name + "~lock");
	}

	public String getName() {
		return name;
	}

	/**
	 * {@inheritDoc} This implementation simply returns the RedisTemplate used for configuring the cache, giving access to
	 * the underlying Redis store.
	 */
	public Object getNativeCache() {
		return redisTemplate;
	}

	@SuppressWarnings("unchecked")
	public ValueWrapper get(final Object key) {
		return (ValueWrapper) redisTemplate.execute(new RedisCallback<ValueWrapper>() {

			public ValueWrapper doInRedis(RedisConnection connection) throws DataAccessException {
				waitForLock(connection);
				byte[] bs = connection.get(computeKey(key));
				Object value = redisTemplate.getValueSerializer() != null ? redisTemplate.getValueSerializer().deserialize(bs) : bs;
				return (bs == null ? null : new SimpleValueWrapper(value));
			}
		}, true);
	}

	/**
	 * Return the value to which this cache maps the specified key, generically specifying a type that return value will
	 * be cast to.
	 * 
	 * @param key
	 * @param type
	 * @return
	 * @see DATAREDIS-243
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(Object key, Class<T> type) {
		ValueWrapper wrapper = get(key);
		return wrapper == null ? null : (T) wrapper.get();
	}

	@SuppressWarnings("unchecked")
	public void put(final Object key, final Object value,final Long expireTime) {

		final byte[] keyBytes = computeKey(key);
		final byte[] valueBytes = convertToBytesIfNecessary(redisTemplate.getValueSerializer(), value);

		redisTemplate.execute(new RedisCallback<Object>() {
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				waitForLock(connection);
				connection.multi();
				connection.set(keyBytes, valueBytes);
				connection.zAdd(setName, 0, keyBytes);
				if (expireTime!=null && expireTime!=0L) {
					connection.expire(keyBytes, expireTime);
					connection.expire(setName, expireTime);
				}
				connection.exec();
				return null;
			}
		}, true);
	}

	@SuppressWarnings("unchecked")
	public ValueWrapper putIfAbsent(Object key, final Object value,final Long expireTime) {

		final byte[] keyBytes = computeKey(key);
		final byte[] valueBytes = convertToBytesIfNecessary(redisTemplate.getValueSerializer(), value);

		return toWrapper(redisTemplate.execute(new RedisCallback<Object>() {
			public Object doInRedis(RedisConnection connection) throws DataAccessException {

				waitForLock(connection);

				Object resultValue = value;
				boolean valueWasSet = connection.setNX(keyBytes, valueBytes);
				if (valueWasSet) {
					connection.zAdd(setName, 0, keyBytes);
					if (expireTime!=null && expireTime!=0L) {
						connection.expire(keyBytes, expireTime);
						connection.expire(setName, expireTime);
					}
				} else {
					resultValue = deserializeIfNecessary(redisTemplate.getValueSerializer(), connection.get(keyBytes));
				}

				return resultValue;
			}
		}, true));
	}

	private ValueWrapper toWrapper(Object value) {
		return (value != null ? new SimpleValueWrapper(value) : null);
	}

	@SuppressWarnings("unchecked")
	public void evict(Object key) {
		final byte[] k = computeKey(key);
		redisTemplate.execute(new RedisCallback<Object>() {
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				connection.del(k);
				// remove key from set
				connection.zRem(setName, k);
				return null;
			}
		}, true);
	}

	@SuppressWarnings("unchecked")
	public void clear() {
		// need to del each key individually
		redisTemplate.execute(new RedisCallback<Object>() {
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				// another clear is on-going
				if (connection.exists(cacheLockName)) {
					return null;
				}
				try {
					connection.set(cacheLockName, cacheLockName);
					int offset = 0;
					boolean finished = false;
					do {
						// need to paginate the keys
						Set<byte[]> keys = connection.zRange(setName, (offset) * PAGE_SIZE, (offset + 1) * PAGE_SIZE - 1);
						finished = keys.size() < PAGE_SIZE;
						offset++;
						if (!keys.isEmpty()) {
							connection.del(keys.toArray(new byte[keys.size()][]));
						}
					} while (!finished);

					connection.del(setName);
					return null;

				} finally {
					connection.del(cacheLockName);
				}
			}
		}, true);
	}

	@SuppressWarnings("unchecked")
	private byte[] computeKey(Object key) {
		byte[] keyBytes = convertToBytesIfNecessary(redisTemplate.getKeySerializer(), key);
		if (prefix == null || prefix.length == 0) {
			return keyBytes;
		}
		byte[] result = Arrays.copyOf(prefix, prefix.length + keyBytes.length);
		System.arraycopy(keyBytes, 0, result, prefix.length, keyBytes.length);
		return result;
	}

	private boolean waitForLock(RedisConnection connection) {
		boolean retry;
		boolean foundLock = false;
		do {
			retry = false;
			if (connection.exists(cacheLockName)) {
				foundLock = true;
				try {
					Thread.sleep(WAIT_FOR_LOCK);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				retry = true;
			}
		} while (retry);

		return foundLock;
	}

	private byte[] convertToBytesIfNecessary(RedisSerializer<Object> serializer, Object value) {

		if (serializer == null && value instanceof byte[]) {
			return (byte[]) value;
		}

		return serializer.serialize(value);
	}

	private Object deserializeIfNecessary(RedisSerializer<byte[]> serializer, byte[] value) {

		if (serializer != null) {
			return serializer.deserialize(value);
		}

		return value;
	}

}
