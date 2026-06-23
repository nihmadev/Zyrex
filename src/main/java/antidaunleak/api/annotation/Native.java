package antidaunleak.api.annotation;

public @interface Native {
    Type type() default Type.STANDARD;

    enum Type {
        STANDARD,
        VMProtectBeginVirtualization,
        VMProtectBeginMutation,
        VMProtectBeginUltra,
    }
}
