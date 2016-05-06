package com.dotmarketing.portlets.rules.parameter.type.constraint;


/**
 * The String keys used in this Class correspond to values in Core-Web. Edit with extreme care.
 * @author Geoff M. Granum
 */
public final class StandardConstraints {

    public static final TypeConstraint required = new TypeConstraint("required", StandardConstraintFunctions.required);
    public static final TypeConstraint numeric = new TypeConstraint("numeric", StandardConstraintFunctions.numeric());
    public static final TypeConstraint integral = new TypeConstraint("integral", StandardConstraintFunctions.integral());

    public static TypeConstraint minLength(int minLength){
        return new TypeConstraint("minLength", StandardConstraintFunctions.minLength(minLength), minLength);
    }

    public static TypeConstraint maxLength(int maxLength) {
        return new TypeConstraint("maxLength", StandardConstraintFunctions.maxLength(maxLength), maxLength);
    }

    public static TypeConstraint maxValue(double maxValue) {
        return maxValue(maxValue, true );
    }
    
    public static TypeConstraint maxValue(double maxValue, boolean inclusive) {
        return new TypeConstraint("maxValue", StandardConstraintFunctions.maxValue(maxValue, inclusive), maxValue);
    }

    public static TypeConstraint minValue(double minValue) {
        return minValue(minValue, true);
    }

    public static TypeConstraint minValue(double minValue, boolean inclusive) {
        return new TypeConstraint("minValue", StandardConstraintFunctions.minValue(minValue, inclusive), minValue);
    }

    public static TypeConstraint maxValue(int maxValue) {
        return maxValue(maxValue, true);
    }

    public static TypeConstraint maxValue(int maxValue, boolean inclusive) {
        return new TypeConstraint("maxValue", StandardConstraintFunctions.maxValue(maxValue, inclusive), maxValue);
    }

    public static TypeConstraint minValue(int minValue) {
        return minValue(minValue, true);
    }

    public static TypeConstraint minValue(int minValue, boolean inclusive) {
        return new TypeConstraint("minValue", StandardConstraintFunctions.minValue(minValue, inclusive), minValue);
    }

}
 
