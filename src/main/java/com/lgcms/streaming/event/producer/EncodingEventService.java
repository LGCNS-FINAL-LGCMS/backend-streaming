package com.lgcms.streaming.event.producer;

import com.lgcms.streaming.common.kafka.dto.EncodingStatus;
import com.lgcms.streaming.common.kafka.dto.KafkaEvent;
import com.lgcms.streaming.common.kafka.dto.EncodingSuccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EncodingEventService {

    @Value("${spring.application.name}")
    private String applicationName;

    private final KafkaTemplate kafkaTemplate;

    public void EncodingSuccessEvent(EncodingSuccess videoEncodingSuccess){
        String eventId = applicationName + UUID.randomUUID().toString();

        KafkaEvent kafkaEvent = KafkaEvent.builder()
                .eventId(eventId)
                .eventTime(LocalDateTime.now().toString())
                .eventType("ENCODING_SUCCESS")
                .data(videoEncodingSuccess)
                .build();

        kafkaTemplate.send("ENCODING_SUCCESS_LESSON",kafkaEvent);
    }

    public void EncodingFailEvent(EncodingStatus encodingStatus){
        String eventId = applicationName + UUID.randomUUID().toString();

        KafkaEvent kafkaEvent = KafkaEvent.builder()
                .eventId(eventId)
                .eventType("ENCODING_STATUS")
                .eventTime(LocalDateTime.now().toString())
                .data(encodingStatus)
                .build();

        kafkaTemplate.send("NOTIFICATION", kafkaEvent);

    }
}
