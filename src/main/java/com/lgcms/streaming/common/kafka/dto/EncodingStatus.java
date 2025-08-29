package com.lgcms.streaming.common.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncodingStatus {

    private Long memberId;
    private String lectureId;
    private String lectureName;
    private String status;
}
