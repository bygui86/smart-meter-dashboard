package io.spring.demo.meter.dashboard;

import io.spring.demo.meter.dashboard.generator.ZoneDescriptor;
import io.spring.demo.meter.dashboard.sampling.PowerGridSample;
import reactor.core.publisher.Flux;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@EnableConfigurationProperties(DashboardProperties.class)
public class SmartMeterDashboardApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartMeterDashboardApplication.class, args);
	}

	@Bean
	public ApplicationRunner createMongoCollection(ReactiveMongoTemplate mongoTemplate, DashboardProperties properties) {
		return args -> {
			Flux<ZoneDescriptor> zoneDescriptors = WebClient
					.create(properties.getGenerator().getServiceUrl())
					.get().uri("/descriptor/zones")
					.retrieve().bodyToFlux(ZoneDescriptor.class);
			CollectionOptions options = CollectionOptions.empty().size(104857600).capped();

			mongoTemplate
					.createCollection(ZoneDescriptor.class)
					.thenMany(zoneDescriptors.flatMap(zd -> mongoTemplate.insert(zd)))
					.then(mongoTemplate.createCollection(PowerGridSample.class, options))
					.block();
		};
	}
}
