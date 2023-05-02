package com.fishbowl.lambdas.updater.models;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Builder
@Data
@EqualsAndHashCode
public class Currencies {
    List<Currency> currencies;
}
