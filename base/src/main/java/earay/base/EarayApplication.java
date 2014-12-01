package earay.base;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.SessionFactoryFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.Set;

import javax.inject.Singleton;

import org.reflections.Reflections;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.config.SwaggerConfig;
import com.wordnik.swagger.jaxrs.config.DefaultJaxrsScanner;
import com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider;
import com.wordnik.swagger.jaxrs.listing.ApiListingResourceJSON;
import com.wordnik.swagger.jaxrs.listing.ResourceListingProvider;
import com.wordnik.swagger.jaxrs.reader.DefaultJaxrsApiReader;
import com.wordnik.swagger.reader.ClassReaders;

import earay.base.backend.model.AbstractModel;
import earay.base.frontend.resource.GroovyREST;

public class EarayApplication extends Application<EarayConfiguration> {
	
	private HibernateBundle<EarayConfiguration> hibernateBundle;
	
	@Override
	public String getName() {
		return "Earay";
	}
	
	@Override
	public void initialize(Bootstrap<EarayConfiguration> bootstrap) {
		Reflections reflections = new Reflections("earay");
		Set<Class<? extends AbstractModel>> modelClasses = reflections.getSubTypesOf(AbstractModel.class);
		if (!modelClasses.isEmpty()) {
			bootstrap.addBundle(hibernateBundle = new HibernateBundle<EarayConfiguration>(
					ImmutableList.<Class<?>>builder().add(AbstractModel.class).addAll(modelClasses.iterator()).build(),
					new SessionFactoryFactory()) {
				@Override
				public DataSourceFactory getDataSourceFactory(EarayConfiguration configuration) {
					return configuration.getDatabase();
				}
			});
			bootstrap.addBundle(new MigrationsBundle<EarayConfiguration>() {
				@Override
				public DataSourceFactory getDataSourceFactory(EarayConfiguration configuration) {
					return configuration.getDatabase();
				}
			});
		}
		bootstrap.addBundle(new AssetsBundle("/swagger-ui/", "/", "index.html"));
	}
	
	@Override
	public void run(EarayConfiguration config, Environment environment) throws Exception {
		environment.getApplicationContext().setContextPath(config.getApplicationSettings().getContextPath());
		environment.getObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		Injector injector = Guice.createInjector(new EarayModule(
				hibernateBundle != null ? hibernateBundle.getSessionFactory() : null, 
				config, environment.metrics()));
		environment.jersey().setUrlPattern("/api/*");
		environment.jersey().register(new GroovyREST(injector));
		registerResources(environment, injector, GroovyREST.class.getPackage().getName());
		for (EarayProject project : config.getProjects()) {
			registerResources(environment, injector, project.getResourcePackage());
		}
		environment.jersey().register(new ApiListingResourceJSON());
		environment.jersey().register(new ApiDeclarationProvider());
		environment.jersey().register(new ResourceListingProvider());
		ScannerFactory.setScanner(new DefaultJaxrsScanner());
		ClassReaders.setReader(new DefaultJaxrsApiReader());
		SwaggerConfig swaggerConfig = ConfigFactory.config();
		swaggerConfig.setApiVersion("1");
		swaggerConfig.setBasePath("/api");
	}
	
	private void registerResources(Environment environment, Injector injector, String resourcePackage) {
		Reflections reflections = new Reflections(resourcePackage);
		Set<Class<?>> resourceClasses = reflections.getTypesAnnotatedWith(Singleton.class);
		for (Class<? extends Object> rc : resourceClasses)
			environment.jersey().register(injector.getInstance(rc));
	}

	public static void main(String[] args) throws Exception {
		new EarayApplication().run(args);
	}
}
