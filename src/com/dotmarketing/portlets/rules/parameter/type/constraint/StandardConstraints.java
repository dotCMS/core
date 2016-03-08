package com.dotmarketing.portlets.rules.parameter.type.constraint;


/**
 * The String keys used in this Class correspond to values in Core-Web. Edit with extreme care.
 * @author Geoff M. Granum
 */
public final class StandardConstraints {

    public static final TypeConstraint required = new TypeConstraint("required", StandardConstraintFunctions.required);
    public static final TypeConstraint numeric = new TypeConstraint("numeric", StandardConstraintFunctions.numeric());
    public static final TypeConstraint integral = new TypeConstraint("integral", StandardConstraintFunctions.integral());

    public static TypeConstraint minLength(int min){
        return new TypeConstraint("minLength", StandardConstraintFunctions.minLength(min), min);
    }

    public static TypeConstraint maxLength(int max) {
        return new TypeConstraint("maxLength", StandardConstraintFunctions.maxLength(max), max);
    }

    public static TypeConstraint max(double max) {
        return max(max, true );
    }
    
    public static TypeConstraint max(double max, boolean inclusive) {
        return new TypeConstraint("max", StandardConstraintFunctions.max(max, inclusive), max);
    }

    public static TypeConstraint min(double min) {
        return min(min, true);
    }

    public static TypeConstraint min(double min, boolean inclusive) {
        return new TypeConstraint("min", StandardConstraintFunctions.min(min, inclusive), min);
    }

    public static TypeConstraint max(int max) {
        return max(max, true);
    }

    public static TypeConstraint max(int max, boolean inclusive) {
        return new TypeConstraint("max", StandardConstraintFunctions.max(max, inclusive), max);
    }

    public static TypeConstraint min(int min) {
        return min(min, true);
    }

    public static TypeConstraint min(int min, boolean inclusive) {
        return new TypeConstraint("min", StandardConstraintFunctions.min(min, inclusive), min);
    }

}
 
