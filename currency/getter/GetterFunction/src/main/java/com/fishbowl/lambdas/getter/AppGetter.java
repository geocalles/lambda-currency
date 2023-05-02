package com.fishbowl.lambdas.getter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fishbowl.lambdas.getter.models.Currency;
import com.fishbowl.lambdas.getter.models.*;
import com.fishbowl.lambdas.updater.exception.LambdaException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Handler for requests to Lambda function.
 */
public class AppGetter implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private LambdaLogger logger;
    private Jedis redis;

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        logger = context.getLogger();
        logger.log(gson.toJson("Lambda is called: " + context.getFunctionName()));
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(headers);

        try {

            if (!input.getHttpMethod().equals("GET")) {
                return response
                        .withBody("Method not implemented.")
                        .withStatusCode(400);
            }
            logger.log(gson.toJson("Setup Redis Config "));
            setupRedisConfig();

            if (Objects.equals(input.getPath(), "/countries-currency")) {
                logger.log(gson.toJson("Getting Countries Currency Data "));
                String code = input.getQueryStringParameters() != null ? input.getQueryStringParameters().get("code"):null;
                CountriesCurrency listCountriesCurrency = getCountriesCurrency();
                if (code != null && !code.equals("")) {
                    CountryCurrency res = listCountriesCurrency.getCountriesCurrency()
                            .stream().filter(c -> c.getCurrencyCode().equals(code)).findFirst().orElse(null);
                    return response
                            .withBody(new Gson().toJson(res, CountryCurrency.class))
                            .withStatusCode(200);
                }
                return response
                        .withBody(new Gson().toJson(listCountriesCurrency.getCountriesCurrency().toArray(), CountriesCurrency[].class))
                        .withStatusCode(200);
            } else if (input.getPath().equals("/countries")) {

                Countries countries = handleCountries(input);
                return response
                        .withBody(new Gson().toJson(countries, Countries.class))
                        .withStatusCode(200);
            } else if (input.getPath().equals("/currencies")) {
                Currencies currencies = handleCurrencies(input);
                return response
                        .withBody(new Gson().toJson(currencies, Currencies.class))
                        .withStatusCode(200);
            }

            return response
                    .withBody("Method not implemented.")
                    .withStatusCode(400);
        } catch (Exception e) {
            logger.log(gson.toJson("HandleRequest exception: " + e.getMessage()));
            return response
                    .withBody("{}")
                    .withStatusCode(500);
        }
    }

    private Currencies handleCurrencies(APIGatewayProxyRequestEvent input) {
        Currencies currencies;
        logger.log(gson.toJson("Getting countries Data "));
        String code = input.getQueryStringParameters() != null ? input.getQueryStringParameters().get("code"):null;
        currencies = new Gson().fromJson(getCurrencies(), Currencies.class);
        if (code != null && !code.equals("")) {
            logger.log(gson.toJson("Getting currencies by code " + code));
            var countryFound = currencies.getCurrencies()
                    .stream()
                    .filter(c -> c.getCode().equals(code))
                    .collect(Collectors.toList());
            currencies.setCurrencies(countryFound);
        }
        return currencies;
    }

    private Countries handleCountries(APIGatewayProxyRequestEvent input) {
        Countries countries;
        logger.log(gson.toJson("Getting countries Data "));
        String code = input.getQueryStringParameters() != null ? input.getQueryStringParameters().get("code"):null;
        countries = new Gson().fromJson(getCountries(), Countries.class);
        if (code != null && !code.equals("")) {
            logger.log(gson.toJson("Getting countries by code " + code));
            var countryFound = countries.getCountries()
                    .stream()
                    .filter(c -> c.getCode().equals(code))
                    .collect(Collectors.toList());
            countries.setCountries(countryFound);
        }
        return countries;
    }

    private CountriesCurrency getCountriesCurrency() {
        List<Country> countries = new Gson().fromJson(getCountries(), Countries.class).getCountries();
        List<Currency> currencies = new Gson().fromJson(getCurrencies(), Currencies.class).getCurrencies();
        List<CountryCurrency> listCountriesAndCurrency = new ArrayList<>();
        for (Country country : countries) {
            String countryCode = country.getCode();
            Currency currency = currencies.stream().filter(c -> c.getCountries().stream().anyMatch(ct -> ct.getCode().equals(countryCode))).findFirst().orElse(null);
            if (currency != null) {
                listCountriesAndCurrency.add(CountryCurrency.builder()
                        .code(countryCode)
                        .label(country.getName())
                        .currencySymbol(currency.getSymbol())
                        .currencyCode(currency.getCode())
                        .currencyName(currency.getName())
                        .locale(country.getLocale())
                        .build());
            }
        }
        return CountriesCurrency.builder().countriesCurrency(listCountriesAndCurrency).build();
    }

    private String getCountries() {
        var redisCountryKey = System.getenv("REDIS_COUNTRY_KEY");
        return redis.get(redisCountryKey);
    }

    private String getCurrencies() {
        var redisCountryKey = System.getenv("REDIS_CURRENCY_KEY");
        return redis.get(redisCountryKey);
    }

    private void setupRedisConfig() throws LambdaException {
        String redisHost = System.getenv("REDIS_HOST");
        String redisPort = System.getenv("REDIS_PORT");
        logger.log(gson.toJson("Redis Host: " + redisHost));
        logger.log(gson.toJson("Redis Port: " + redisPort));

        try (JedisPool jedisPool = new JedisPool(redisHost, Integer.parseInt(redisPort))) {
            redis = jedisPool.getResource();
        } catch (Exception e) {
            logger.log(gson.toJson("Error on redis connection: " + e.getMessage()));
            throw new LambdaException("Something is wrong.");
        }

        String redisDB = System.getenv("REDIS_DB");
        if (redisDB == null || "".equals(redisDB)) {
            logger.log(gson.toJson("Redis DB is empty"));
            throw new LambdaException("DB empty");
        }

        redis.select(Integer.parseInt(redisDB));
        String redisCurrencyKey = System.getenv("REDIS_CURRENCY_KEY");
        if (redisCurrencyKey == null || "".equals(redisCurrencyKey)) {
            logger.log(gson.toJson("Currency key not filled."));
            throw new LambdaException("Error on setup currency.");
        }

        String redisCountryKey = System.getenv("REDIS_COUNTRY_KEY");
        if (redisCountryKey == null || "".equals(redisCountryKey)) {
            logger.log(gson.toJson("Country key not filled."));
            throw new LambdaException("Error on setup country.");
        }
    }


}
