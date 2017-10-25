package io.spring.demo.meter.dashboard.sampling;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.spring.demo.meter.dashboard.generator.ElectricityMeasure;
import io.spring.demo.meter.dashboard.generator.MeasuresCollector;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

@Service
public class PowerGridSampler implements SmartLifecycle {

	private final MeasuresCollector measuresCollector;

	private final PowerGridSampleRepository repository;

	private final Object monitor = new Object();

	private final AtomicReference<Instant> currentTimestamp = new AtomicReference<>(Instant.now());

	private Disposable subscription;


	public PowerGridSampler(MeasuresCollector measuresCollector, PowerGridSampleRepository repository) {
		this.measuresCollector = measuresCollector;
		this.repository = repository;
	}

	@Override
	public void start() {
		synchronized (this.monitor) {
			if (!this.isRunning()) {
				this.subscription = sampleMeasuresForPowerGrid().subscribe();
			}
		}
	}

	@Override
	public void stop() {
		synchronized (this.monitor) {
			if (this.isRunning()) {
				this.subscription.dispose();
			}
		}
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable runnable) {
		stop();
		runnable.run();
	}

	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public boolean isRunning() {
		synchronized (this.monitor) {
			return this.subscription != null && !this.subscription.isDisposed();
		}
	}

	private Flux<PowerGridSample> sampleMeasuresForPowerGrid() {
		Flux<ElectricityMeasure> measures = measuresCollector.getElectricityMeasures();

		Flux<PowerGridSample> samples = measures
				// buffer until the timestamp of the measures changes
				.windowUntil(measure -> {
					if (this.currentTimestamp.get().isBefore(measure.getTimestamp())) {
						this.currentTimestamp.set(measure.getTimestamp());
						return true;
					}
					return false;
				}, true)

				// group measures by zoneIds + timestamp
				.flatMap(window -> window.groupBy(measure ->
						new PowerGridSampleKey(measure.getZoneId(), measure.getTimestamp())))

				// for each group, reduce all measures into a PowerGrid sample for that timestamp
				.flatMap(windowForZone -> {
					PowerGridSampleKey key = windowForZone.key();
					PowerGridSample initial = new PowerGridSample(key.zoneId, key.timestamp);
					return windowForZone.reduce(initial,
							(powerGridSample, measure) -> {
								powerGridSample.addMeasure(measure);
								return powerGridSample;
							});
				});

		// save the generated samples and return them
		return this.repository.saveAll(samples);
	}

}
