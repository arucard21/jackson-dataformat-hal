package io.openapitools.jackson.dataformat.hal.deser;

import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import io.openapitools.jackson.dataformat.hal.annotation.EmbeddedResource;
import io.openapitools.jackson.dataformat.hal.annotation.Link;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

/**
 * Modelling reserved HAL properties namely <code>_links</code> and <code>_embedded</code>.
 */
public enum ReservedProperty {

    LINKS("_links", Link.class), EMBEDDED("_embedded", EmbeddedResource.class);

    private final String name;
    private final UUID prefix = UUID.randomUUID();
    private final Class<? extends Annotation> annotation;
    private final Method valueMethod;

    ReservedProperty(String name, Class<? extends Annotation> annotation) {
        this.name = name;
        this.annotation = annotation;
        try {
            valueMethod = annotation.getDeclaredMethod("value");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public String getPropertyName() {
        return name;
    }

    public String alternateName(BeanPropertyDefinition bpd, CurieMap map) {
        String originalName = bpd.getName();

        AnnotatedMember annotatedMember = firstNonNull(bpd.getField(), bpd.getSetter(), bpd.getGetter());
        if (annotatedMember == null) {
            return originalName;
        }

        Annotation o = annotatedMember.getAnnotation(annotation);
        if (o == null) {
            return originalName;
        }

        String name;
        try {
            String alternateName = (String) valueMethod.invoke(o);
            name = alternateName.isEmpty() ? originalName : alternateName;
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        if (o.annotationType() == Link.class) {
            String curiePrefix = ((Link) o).curie();
            if (!curiePrefix.isEmpty()) {
                Optional<URI> resolved = map.resolve(curiePrefix + ":" + name);
                return alternateName(resolved.map(URI::toString).orElse(name));
            }
        }

        return alternateName(name);
    }

    public String alternateName(String originalName) {
        return prefix.toString() + ":" + originalName;
    }

    @SafeVarargs
    static <T> T firstNonNull(T... vals) {
        for (T v : vals) {
            if (v != null)
                return v;
        }

        return null;
    }
}
