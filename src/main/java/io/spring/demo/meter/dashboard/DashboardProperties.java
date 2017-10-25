package io.spring.demo.meter.dashboard;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("meter.dashboard")
public class DashboardProperties {

	private Generator generator = new Generator();

	public Generator getGenerator() {
		return generator;
	}

	public static class Generator {

		private String serviceUrl = "http://localhost:8081";

		public String getServiceUrl() {
			return serviceUrl;
		}

		public void setServiceUrl(String serviceUrl) {
			this.serviceUrl = serviceUrl;
		}
	}

}
