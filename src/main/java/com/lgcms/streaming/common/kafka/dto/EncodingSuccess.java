package com.lgcms.streaming.common.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncodingSuccess {

    private String status;
    private String lectureId;
    private Long memberId;
    private String lessonId;
    private Integer duration;
    private String videoUrl;
    private String thumbnailUrl;

}
