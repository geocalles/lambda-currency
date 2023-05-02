package com.fishbowl.lambdas.updater.models;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Builder
@Data
@EqualsAndHashCode
public class Currency {
    private String code;
    private String symbol;
    private String name;
    private List<Country> countries;

}
