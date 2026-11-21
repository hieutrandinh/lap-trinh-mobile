package org.example.be.core.util;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;

/**
 * Only ever invoked by Jackson when the JSON property is actually present (Jackson
 * silently skips absent properties), so reaching here - even for a JSON null - means
 * the client sent the field. That's what lets {@link JsonNullable} tell "omitted" apart
 * from "explicit null".
 */
public class JsonNullableDeserializer extends ValueDeserializer<JsonNullable<?>> {

    private ValueDeserializer<Object> contentDeserializer;

    public JsonNullableDeserializer() {
    }

    private JsonNullableDeserializer(ValueDeserializer<Object> contentDeserializer) {
        this.contentDeserializer = contentDeserializer;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        JavaType wrapperType = property.getType();
        JavaType contentType = wrapperType.containedType(0);
        ValueDeserializer<Object> deser = ctxt.findContextualValueDeserializer(contentType, property);
        return new JsonNullableDeserializer(deser);
    }

    @Override
    public JsonNullable<?> deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        Object value = contentDeserializer.deserialize(p, ctxt);
        return JsonNullable.of(value);
    }

    @Override
    public JsonNullable<?> getNullValue(DeserializationContext ctxt) {
        return JsonNullable.of(null);
    }

}
