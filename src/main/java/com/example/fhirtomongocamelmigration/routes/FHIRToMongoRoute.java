package com.example.fhirtomongocamelmigration.routes;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.util.List;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Component;

@Component
public class FHIRToMongoRoute extends RouteBuilder {

	private static final String BUNDLE_HEADER = "BundleHeader";
	private static final String HAS_NEXT_HEADER = "HasNext";
	private static final IParser jsonParser = FhirContext.forR4Cached().newJsonParser();

	@Value("${camel.component.fhir.server-url}/Patient")
	private String patientUrl;
	@Value("${javascriptFileLocation:#{null}}")
	private String javascriptFileLocation;

	@Override
	public void configure() {

		from(String.format("fhir:load-page/byUrl?returnType=org.hl7.fhir.r4.model.Bundle&url=%s", patientUrl))
			.marshal().fhirJson()
			.setHeader(HAS_NEXT_HEADER, simple("true"))
			.log("Page loaded, start processing...")
			.to("direct:processPage");

		from("direct:processPage")
			.process(exchange -> {
				Bundle bundle = jsonParser.parseResource(Bundle.class, exchange.getIn().getBody(String.class));
				exchange.getIn().setHeader(HAS_NEXT_HEADER, bundle.getLink(IBaseBundle.LINK_NEXT) != null);
				exchange.getIn().setHeader(BUNDLE_HEADER, bundle);
				exchange.getIn().setBody(bundle.getEntry().stream().map(bundleEntryComponent ->
					jsonParser.encodeResourceToString(bundleEntryComponent.getResource())).toList());
			})
			.choice()
				.when((exchange) -> javascriptFileLocation != null)
					.log("Processing JS file : " + javascriptFileLocation)
					.to(String.format("language:js:resource:file:%s?transform=true&resultType=String", javascriptFileLocation))
					.process(exchange -> {
						String body = exchange.getIn().getBody(String.class);
						List<Object> patients = JsonParserFactory.getJsonParser().parseList(body);
						exchange.getIn().setBody(patients);
					})
			.end()
			.log("Storing patients in mongodb")
			.split(body())
			.to("mongodb:mongoBean?database=Cluster0&collection=Patients&operation=insert")
			.end()
			.choice()
				.when(exchange -> exchange.getIn().getHeader(HAS_NEXT_HEADER, Boolean.class))
					.to("direct:loadNextPage")
				.otherwise()
					.process(exchange -> new Thread(() -> {
						try {
							exchange.getContext().stop();
							System.exit(0);
						} catch (Exception e) {
							log.error("Error during shutdown", e);
						}
					}).start());

		from("direct:loadNextPage")
			.setHeader("CamelFhir.bundle", simple("${header." + BUNDLE_HEADER + "}"))
			.to("fhir:load-page/next")
			.marshal().fhirJson()
			.log("Page loaded, start processing...")
			.to("direct:processPage");
	}
}
