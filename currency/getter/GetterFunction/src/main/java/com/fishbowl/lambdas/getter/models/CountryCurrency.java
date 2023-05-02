package com.fishbowl.lambdas.getter.models;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Builder
@Data
@EqualsAndHashCode
public class CountryCurrency {
    private String code;
    private String label;
    private String currencySymbol;
    private String currencyCode;
    private String currencyName;
    private String locale;

}
