package com.fishbowl.lambdas.updater.models;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Builder
@Data
@EqualsAndHashCode
public class Country {
    private String iso;
    private String code;
    private String name;
    private String locale;

}
