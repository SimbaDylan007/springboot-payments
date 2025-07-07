package com.yourcompany.payments.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.client.RestTemplate;
import org.springframework.ws.client.core.WebServiceTemplate;

@Configuration
public class AppConfig {

    // Inject the WSDL URL from application.properties
    @Value("${egress.wsdl-url}")
    private String egressWsdlUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("com.yourcompany.payments.wsdl");
        return marshaller;
    }

    @Bean
    public WebServiceTemplate webServiceTemplate(Jaxb2Marshaller marshaller) {
        WebServiceTemplate webServiceTemplate = new WebServiceTemplate();

        // FIX: Set the default URI for all SOAP requests.
        // The service endpoint is usually the WSDL URL without the "?wsdl" part.
        webServiceTemplate.setDefaultUri(egressWsdlUrl.replace("?wsdl", ""));

        webServiceTemplate.setMarshaller(marshaller);
        webServiceTemplate.setUnmarshaller(marshaller);
        return webServiceTemplate;
    }
}