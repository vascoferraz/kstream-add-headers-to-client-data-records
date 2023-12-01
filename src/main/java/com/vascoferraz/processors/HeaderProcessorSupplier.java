package com.vascoferraz.processors;

import com.vascoferraz.ClientDataKey;
import com.vascoferraz.ClientDataValue;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;

public class HeaderProcessorSupplier implements ProcessorSupplier<ClientDataKey, ClientDataValue, ClientDataKey, ClientDataValue> {

    private final String headerKey;
    private final String headerValue;

    public HeaderProcessorSupplier(String headerKey, String headerValue) {
        this.headerKey = headerKey;
        this.headerValue = headerValue;
    }

    @Override
    public Processor<ClientDataKey, ClientDataValue, ClientDataKey, ClientDataValue> get() {
        return new HeaderProcessor(headerKey, headerValue);
    }

}
