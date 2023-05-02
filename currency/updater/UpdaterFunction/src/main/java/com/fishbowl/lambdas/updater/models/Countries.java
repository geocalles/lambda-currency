package com.fishbowl.lambdas.updater.models;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Builder
@Data
@EqualsAndHashCode
public class Countries {
    List<Country> countries;
}
