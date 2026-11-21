package org.example.be.rest.advice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.module.SimpleModule;

@Configuration
class ApiJsonConfiguration {
    private static final Pattern MONEY = Pattern.compile("[0-9]{1,9}\\.[0-9]{2}");

    @Bean
    SimpleModule contractJsonModule() {
        SimpleModule module = new SimpleModule("finance-api-contract");
        module.addSerializer(BigDecimal.class, new ValueSerializer<>() {
            @Override
            public void serialize(BigDecimal value, JsonGenerator generator, SerializationContext context) {
                generator.writeString(value.setScale(2, RoundingMode.HALF_UP).toPlainString());
            }
        });
        module.addDeserializer(BigDecimal.class, new ValueDeserializer<>() {
            @Override
            public BigDecimal deserialize(JsonParser parser, DeserializationContext context) {
                if (parser.currentToken() != JsonToken.VALUE_STRING || !MONEY.matcher(parser.getString()).matches()) {
                    return context.reportInputMismatch(BigDecimal.class, "Use a decimal string with two fractional digits.");
                }
                return new BigDecimal(parser.getString());
            }
        });
        module.addSerializer(LocalDateTime.class, new ValueSerializer<>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator generator, SerializationContext context) {
                generator.writeString(value.toInstant(ZoneOffset.UTC).toString());
            }
        });
        return module;
    }
}
