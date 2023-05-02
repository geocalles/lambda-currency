package com.fishbowl.lambdas.getter.models;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Builder
@Data
@EqualsAndHashCode
public class Currencies {
    List<Currency> currencies;
}
