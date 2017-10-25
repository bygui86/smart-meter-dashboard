package io.spring.demo.meter.dashboard;

import io.spring.demo.meter.dashboard.generator.ElectricityMeasure;
import io.spring.demo.meter.dashboard.generator.MeasuresCollector;
import io.spring.demo.meter.dashboard.generator.ZoneDescriptorRepository;
import io.spring.demo.meter.dashboard.sampling.PowerGridSample;
import io.spring.demo.meter.dashboard.sampling.PowerGridSampleRepository;
import org.thymeleaf.spring5.context.webflux.IReactiveSSEDataDriverContextVariable;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.result.view.Rendering;

@Controller
public class DashboardController {

	private final PowerGridSampleRepository powerGridSampleRepository;

	private final ZoneDescriptorRepository zoneDescriptorRepository;

	private final MeasuresCollector measuresCollector;

	public DashboardController(PowerGridSampleRepository powerGridSampleRepository,
			ZoneDescriptorRepository zoneDescriptorRepository, MeasuresCollector measuresCollector) {
		this.powerGridSampleRepository = powerGridSampleRepository;
		this.zoneDescriptorRepository = zoneDescriptorRepository;
		this.measuresCollector = measuresCollector;
	}
	@GetMapping("/")
	public Rendering home() {
		return Rendering
				.view("index")
				.modelAttribute("zones", this.zoneDescriptorRepository.findAll())
				.build();
	}

	@GetMapping("/zones/{zoneId}")
	public Mono<Rendering> displayZone(@PathVariable String zoneId) {
		PageRequest pageRequest = PageRequest.of(0, 20, Sort.by("timestamp").descending());
		Flux<PowerGridSample> latestSamples = this.powerGridSampleRepository.findAllByZoneId(zoneId, pageRequest);

		return this.zoneDescriptorRepository.findById(zoneId)
				.switchIfEmpty(Mono.error(new MissingDataException(zoneId)))
				.map(zoneDescriptor -> Rendering
						.view("zone")
						.modelAttribute("zone", zoneDescriptor)
						.modelAttribute("samples", latestSamples)
						.build());
	}

	@GetMapping(path = "/zones/{zoneId}/updates", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@ResponseBody
	public Flux<PowerGridSample> streamUpdates(@PathVariable String zoneId) {
		return this.powerGridSampleRepository.findWithTailableCursorByZoneId(zoneId);
	}


	@GetMapping(path = "/zones/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Rendering streamZoneEvents() {
		final Flux<ElectricityMeasure> events = this.measuresCollector.getElectricityMeasures()
				.filter(measure -> measure.getPower() == 0);

		final IReactiveSSEDataDriverContextVariable eventsDataDriver =
				new ReactiveDataDriverContextVariable(events, 1);

		return Rendering.view("zone :: #events")
				.modelAttribute("eventStream", eventsDataDriver)
				.build();
	}

}