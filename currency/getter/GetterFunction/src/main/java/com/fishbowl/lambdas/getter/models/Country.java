package com.fishbowl.lambdas.getter.models;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Builder
@Data
@EqualsAndHashCode
public class Country {
    private String iso;
    private String code;
    private String name;
    private String locale;

}
