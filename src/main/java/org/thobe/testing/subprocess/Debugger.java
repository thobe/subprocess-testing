package org.thobe.testing.subprocess;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class Debugger
{
    protected void finish() throws Exception
    {
        // default: do nothing
    }

    public enum Point
    {
        ENTRY, EXIT
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Using
    {
        Class<? extends Debugger> value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Handler
    {
        Point on() default Point.ENTRY;

        Class<?> type() default Placeholder.class;

        String typeName() default "";

        String method();

        Class<?>[] parameters() default {Placeholder.class};

        String signature() default "";
    }

    protected void printStackTrace()
    {
        throw new UnsupportedOperationException( "not implemented" );
    }

    private static class Placeholder
    {
    }
}
