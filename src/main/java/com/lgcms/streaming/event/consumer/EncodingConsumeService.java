package com.lgcms.streaming.event.consumer;

import com.lgcms.streaming.common.kafka.dto.KafkaEvent;
import com.lgcms.streaming.common.kafka.dto.LectureEncodeDto;
import com.lgcms.streaming.common.kafka.utils.KafkaEventFactory;
import com.lgcms.streaming.service.StreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EncodingConsumeService {
    private final StreamingService streamingService;
    private final KafkaEventFactory kafkaEventFactory;

    @KafkaListener(topics = "ENCODING",containerFactory = "defaultFactory")
    public void EncodingEvent(KafkaEvent<?> event){
        LectureEncodeDto lectureEncodeDto = kafkaEventFactory.convert(event, LectureEncodeDto.class);

        Executors.newVirtualThreadPerTaskExecutor().submit(()->{
            streamingService.downloadOriginFile(lectureEncodeDto);
        });
    }
}
