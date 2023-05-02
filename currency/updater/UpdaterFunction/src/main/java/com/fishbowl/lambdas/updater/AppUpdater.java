package com.fishbowl.lambdas.updater;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fishbowl.lambdas.updater.exception.LambdaException;
import com.fishbowl.lambdas.updater.models.Countries;
import com.fishbowl.lambdas.updater.models.Country;
import com.fishbowl.lambdas.updater.models.Currencies;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.neovisionaries.i18n.CountryCode;
import com.neovisionaries.i18n.CurrencyCode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Handler for requests to Lambda function.
 */
public class AppUpdater implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private LambdaLogger logger;
    private String redisCurrencyKey;
    private String redisCountryKey;
    private Jedis redis;

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        logger = context.getLogger();
        logger.log( gson.toJson("Lambda is called: " + context.getFunctionName()  + " \n"));
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(headers);
        try {
            logger.log( gson.toJson("Setup Redis Config \n"));
            setupRedisConfig();
            logger.log( gson.toJson("Getting Countries Data \n"));
            fillCountries();
            logger.log( gson.toJson("Getting Currencies Data \n"));
            fillCurrencies();
            return response
                    .withStatusCode(200)
                    .withBody("{ \"message\": \"success\" }");
        } catch (Exception e) {
            logger.log( gson.toJson("HandleRequest exception: " + e.getMessage()));
            return response
                    .withBody("{}")
                    .withStatusCode(500);
        }
    }

    private void fillCurrencies() {
        List<com.fishbowl.lambdas.updater.models.Currency> list = new ArrayList<>();
        List<java.util.Currency> currencies = java.util.Currency.getAvailableCurrencies()
                .stream()
                .sorted(Comparator.comparing(java.util.Currency::getCurrencyCode))
                .collect(Collectors.toList());
        for (Currency currency : currencies) {
            CurrencyCode currencyCode =  CurrencyCode.getByCode(currency.getCurrencyCode());
            List<Country> countryList = new ArrayList<>();
            if (currencyCode != null)
            {
                List<CountryCode> countryCodeList = currencyCode.getCountryList();
                // For each country.
                for (CountryCode country : countryCodeList)
                {
                    List<Locale> locates =  List.of(Locale.getAvailableLocales());
                    String local = locates.stream().filter(l -> l.toString().contains(country.getAlpha2())).map(Object::toString).findFirst().orElse("");

                    countryList.add(Country.builder()
                            .iso(country.getAlpha3())
                            .code(country.getAlpha2())
                            .locale(local)
                            .name(country.getName())
                            .build());
                }
            }
            String code = currency.getSymbol();
            for (Locale locale : NumberFormat.getAvailableLocales()) {
                NumberFormat numberFormat = NumberFormat.getCurrencyInstance(locale);
                if (code.equals(numberFormat.getCurrency().getCurrencyCode())) {
                    DecimalFormat df = (DecimalFormat) numberFormat;
                    code = df.getDecimalFormatSymbols().getCurrencySymbol();
                    code = code.replace("\u200F", "");
                    break;
                }
            }
            list.add(com.fishbowl.lambdas.updater.models.Currency.builder()
                    .code(currency.getCurrencyCode())
                    .symbol(code)
                    .name(currency.getDisplayName())
                    .countries(countryList)
                    .build());
        }
        var json = new JSONObject(
                new Gson().toJson(Currencies.builder().currencies(list).build(), Currencies.class))
                .toString();
        if (json != null && !"".equals(json)) {
            redis.set(redisCurrencyKey, json);
        }
    }

    private void fillCountries() {
        List<Country> list = new ArrayList<>();
        String[] countryCodes = Locale.getISOCountries();
        List<Locale> locates =  List.of(Locale.getAvailableLocales());
        for (String countryCode : countryCodes) {

            Locale locale = new Locale("", countryCode);
            String iso = locale.getISO3Country();
            String code = locale.getCountry();
            String name = locale.getDisplayCountry(Locale.US);
            String local = locates.stream().filter(l -> l.toString().contains(code)).map(Object::toString).findFirst().orElse("");
            try {
                list.add(Country.builder().iso(iso).code(code).name(name).locale(local)
                        .build());
            } catch (Exception e) {
                logger.log( gson.toJson("Can not get currency for country code: " + code));
            }
        }
        var json = new JSONObject(
                new Gson().toJson(Countries.builder().countries(list).build(), Countries.class))
                .toString();
        if (json != null && !"".equals(json)) {
            redis.set(redisCountryKey, json);
        }
    }

    private void setupRedisConfig() throws LambdaException {
        String redisHost = System.getenv("REDIS_HOST");
        String redisPort = System.getenv("REDIS_PORT");
        logger.log( gson.toJson("Redis Host: " + redisHost + "\n" + "Redis Port: " + redisPort + " \n"));
        JedisPool jedisPool = null;
        try {
            jedisPool = new JedisPool(redisHost, Integer.parseInt(redisPort));
            redis = jedisPool.getResource();
        } catch (Exception e) {
            logger.log(gson.toJson("Error on redis connection: \n" + e.getMessage()));
            throw new LambdaException("Something is wrong.");
        } finally {
            if (jedisPool != null) {
                jedisPool.close();
            }
        }
        logger.log( gson.toJson("Getting Redis Configuration"));
        String redisDB = System.getenv("REDIS_DB");
        if (redisDB == null || "".equals(redisDB)) {
            logger.log( gson.toJson("Redis DB is empty"));
            throw new LambdaException("DB empty");
        }

        redis.select(Integer.parseInt(redisDB));
        redisCurrencyKey = System.getenv("REDIS_CURRENCY_KEY");
        if (redisCurrencyKey == null || "".equals(redisCurrencyKey)) {
            logger.log( gson.toJson("Currency key not filled."));
            throw new LambdaException("Error on setup currency.");
        }

        redisCountryKey = System.getenv("REDIS_COUNTRY_KEY");
        if (redisCountryKey == null || "".equals(redisCountryKey)) {
            logger.log( gson.toJson("Country key not filled."));
            throw new LambdaException("Error on setup country.");
        }

    }
}
