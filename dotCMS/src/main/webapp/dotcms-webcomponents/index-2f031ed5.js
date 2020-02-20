/**
 * Enum to represent Errors in the Binary Field.
 */
var DotBinaryMessageError;
(function (DotBinaryMessageError) {
    DotBinaryMessageError[DotBinaryMessageError["REQUIRED"] = 0] = "REQUIRED";
    DotBinaryMessageError[DotBinaryMessageError["INVALID"] = 1] = "INVALID";
    DotBinaryMessageError[DotBinaryMessageError["URLINVALID"] = 2] = "URLINVALID";
})(DotBinaryMessageError || (DotBinaryMessageError = {}));

export { DotBinaryMessageError as D };
