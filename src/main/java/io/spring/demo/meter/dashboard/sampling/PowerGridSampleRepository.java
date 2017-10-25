package io.spring.demo.meter.dashboard.sampling;


import reactor.core.publisher.Flux;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Tailable;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;

public interface PowerGridSampleRepository extends ReactiveSortingRepository<PowerGridSample, String> {

	@Tailable
	Flux<PowerGridSample> findWithTailableCursorByZoneId(String zoneId);

	Flux<PowerGridSample> findAllByZoneId(String zoneId, Pageable pageable);

}
