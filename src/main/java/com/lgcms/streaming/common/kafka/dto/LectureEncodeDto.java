package com.lgcms.streaming.common.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LectureEncodeDto {
    private String lectureId;
    private String lessonId;
    private Long memberId;
    private String key;
}
