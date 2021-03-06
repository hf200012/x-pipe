package com.ctrip.xpipe.redis.console.health;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.ctrip.xpipe.metric.HostPort;
import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.ClientOptions.DisconnectedBehavior;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.resource.ClientResources;
import com.lambdaworks.redis.resource.DefaultClientResources;
import com.lambdaworks.redis.resource.Delay;

/**
 * @author marsqing
 *
 *         Dec 1, 2016 6:42:01 PM
 */
@Component
public class DefaultRedisSessionManager implements RedisSessionManager {

	private ConcurrentMap<HostPort, RedisSession> sessions = new ConcurrentHashMap<>();

	private ClientResources clientResources;

	public DefaultRedisSessionManager() {
		// ClientResources better be shared among RedisClients
		clientResources = DefaultClientResources.builder()//
				.reconnectDelay(Delay.constant(10, TimeUnit.SECONDS))//
				.build();
	}

	@Override
	public RedisSession findOrCreateSession(String host, int port) {
		HostPort hostPort = new HostPort(host, port);
		RedisSession session = sessions.get(hostPort);

		if (session == null) {
			synchronized (this) {
				session = sessions.get(hostPort);
				if (session == null) {
					session = new RedisSession(findRedisConnection(host, port), hostPort);
					sessions.put(hostPort, session);
				}
			}
		}

		return session;
	}

	private RedisClient findRedisConnection(String host, int port) {
		RedisURI redisUri = new RedisURI(host, port, 2, TimeUnit.SECONDS);

		ClientOptions clientOptions = ClientOptions.builder() //
				.disconnectedBehavior(DisconnectedBehavior.REJECT_COMMANDS)//
				.build();

		RedisClient redis = RedisClient.create(clientResources, redisUri);
		redis.setOptions(clientOptions);

		return redis;
	}
}
