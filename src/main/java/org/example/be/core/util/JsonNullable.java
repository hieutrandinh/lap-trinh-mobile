package org.example.be.core.util;

import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Distinguishes "field omitted" (keep default/current value) from "field explicitly null"
 * (clear the value), which plain nullable fields can't do since Jackson leaves both as null.
 * Requires {@link JsonNullableDeserializer} on the annotated field.
 */
@JsonDeserialize(using = JsonNullableDeserializer.class)
public final class JsonNullable<T> {

    private static final JsonNullable<?> UNDEFINED = new JsonNullable<>(false, null);

    private final boolean present;
    private final T value;

    private JsonNullable(boolean present, T value) {
        this.present = present;
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public static <T> JsonNullable<T> undefined() {
        return (JsonNullable<T>) UNDEFINED;
    }

    public static <T> JsonNullable<T> of(T value) {
        return new JsonNullable<>(true, value);
    }

    public boolean isPresent() {
        return present;
    }

    public T get() {
        return value;
    }

}
