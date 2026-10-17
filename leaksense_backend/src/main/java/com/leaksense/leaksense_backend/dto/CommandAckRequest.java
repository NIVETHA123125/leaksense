package com.leaksense.leaksense_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommandAckRequest {

    private String nodeCode;

    private Boolean success;

    private String note;
}
