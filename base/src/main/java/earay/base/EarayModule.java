package earay.base;

import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.CachingAuthenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.hibernate.SessionFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.yammer.dropwizard.authenticator.LdapAuthenticator;
import com.yammer.dropwizard.authenticator.LdapConfiguration;
import com.yammer.dropwizard.authenticator.ResourceAuthenticator;

import earay.base.EarayConfiguration.AuthType;

@RequiredArgsConstructor
public class EarayModule extends AbstractModule {
	
	@Getter(onMethod = @__({ @Provides }))
	private final SessionFactory sessionFactory;

	@Getter(onMethod = @__({ @Provides }))
	private final EarayConfiguration config;

	@Getter(onMethod = @__({ @Provides }))
	private final MetricRegistry metrics;
	
	@Provides
	Authenticator<BasicCredentials, ?> provideAuthenticator() {
		if (config.getApplicationSettings().getAuthType() == AuthType.Basic) {
			return null;
		} else if (config.getApplicationSettings().getAuthType() == AuthType.Ldap) {
			LdapConfiguration ldapConfiguration = config.getLdapConfiguration();
		    return new CachingAuthenticator<>(metrics,
		            new ResourceAuthenticator(new LdapAuthenticator(ldapConfiguration)),
		            ldapConfiguration.getCachePolicy());
		} else
			return null;
	}

	@Override
	protected void configure() {
//		CacheService cacheService = config.getApplicationSettings().getCache() == CacheType.NOOP ? new NoopCacheService()
//				: new RedisCacheService();
//		log.info("using cache {}", cacheService.getClass());
//		bind(CacheService.class).toInstance(cacheService);
	}

}
