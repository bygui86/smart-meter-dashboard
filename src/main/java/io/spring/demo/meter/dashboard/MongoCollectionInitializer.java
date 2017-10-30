package io.spring.demo.meter.dashboard;

import io.spring.demo.meter.dashboard.generator.ZoneDescriptor;
import io.spring.demo.meter.dashboard.sampling.PowerGridSample;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
class MongoCollectionInitializer implements ApplicationRunner {

	private final ReactiveMongoTemplate mongoTemplate;

	private final DashboardProperties properties;

	public MongoCollectionInitializer(ReactiveMongoTemplate mongoTemplate,
			DashboardProperties properties) {
		this.mongoTemplate = mongoTemplate;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		Flux<ZoneDescriptor> zoneDescriptors = WebClient
				.create(properties.getGenerator().getServiceUrl())
				.get().uri("/descriptor/zones")
				.retrieve().bodyToFlux(ZoneDescriptor.class);
		mongoTemplate.collectionExists(PowerGridSample.class).filter(available -> !available).map(i -> {
			CollectionOptions options = CollectionOptions.empty().size(104857600).capped();
			return mongoTemplate.createCollection(PowerGridSample.class, options).then();

		}).then(createCollection(zoneDescriptors)).block();

	}

	private Mono<Void> createCollection(
			Flux<ZoneDescriptor> zoneDescriptors) {
		return mongoTemplate
				.dropCollection(ZoneDescriptor.class)
				.then(this.mongoTemplate.createCollection(ZoneDescriptor.class)
						.thenMany(zoneDescriptors.flatMap(zd -> mongoTemplate.insert(zd)))
						.then());
	}

}
