package com.vascoferraz.processors;

import com.vascoferraz.ClientDataKey;
import com.vascoferraz.ClientDataValue;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;

import java.util.Arrays;

public class HeaderProcessor implements Processor<ClientDataKey, ClientDataValue, ClientDataKey, ClientDataValue> {

    private final String headerKey;
    private final String headerValue;
    private ProcessorContext<ClientDataKey, ClientDataValue> context;

    public HeaderProcessor(String headerKey, String headerValue) {
        this.headerKey = headerKey;
        this.headerValue = headerValue;
    }

    @Override
    public void init(ProcessorContext<ClientDataKey, ClientDataValue> context) {
        this.context = context;
    }

    @Override
    public void process(Record<ClientDataKey, ClientDataValue> record) {
        if (record.value().getAge() % 2 == 1) {
            System.out.println(headerKey + Arrays.toString(headerValue.getBytes()) + record.value());
            record.headers().add(headerKey, headerValue.getBytes());
            context.forward(record);
        }
        else if (record.value().getAge() % 2 == 0) {
            System.out.println(headerKey + Arrays.toString(headerValue.getBytes()) + record.value());
            record.headers().add(headerKey, headerValue.getBytes());
            context.forward(record);
        }
        else
            context.forward(record);
    }

    @Override
    public void
    close() {
        // no resources to release
    }

}
