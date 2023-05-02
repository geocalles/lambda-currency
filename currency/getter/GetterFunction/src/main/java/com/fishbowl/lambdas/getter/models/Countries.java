package com.fishbowl.lambdas.getter.models;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;
@Builder
@Data
@EqualsAndHashCode
public class Countries {
    List<Country> countries;
}
