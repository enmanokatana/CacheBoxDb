package org.athens;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.function.Predicate;

/**
 * Validation framework for CacheBox to enforce data constraints and rules.
 */
public class CacheValidation {

    /**
     * Represents a validation constraint for a specific key in the CacheBox.
     */
    public static class ValidationRule<T> {
        private final String key;
        private final Class<T> type;
        private final List<Predicate<T>> validators;
        private final boolean isRequired;

        private ValidationRule(String key, Class<T> type, boolean isRequired) {
            this.key = key;
            this.type = type;
            this.validators = new ArrayList<>();
            this.isRequired = isRequired;
        }

        public String getKey() {
            return key;
        }

        /**
         * Create a new validation rule for a specific key.
         * @param key The key to apply validation to
         * @param type The expected data type
         * @return A ValidationRule builder
         */
        public static <T> ValidationRule<T> forKey(String key, Class<T> type) {
            return new ValidationRule<>(key, type, false);
        }

        /**
         * Mark the key as required.
         * @return The current ValidationRule
         */
        public ValidationRule<T> required() {
            return new ValidationRule<>(key, type, true);
        }

        /**
         * Add a custom validation predicate.
         * @param validator Custom validation logic
         * @return The current ValidationRule
         */
        public ValidationRule<T> validate(Predicate<T> validator) {
            validators.add(validator);
            return this;
        }

        /**
         * Add a regex pattern validation for string values.
         * @param pattern Regex pattern to match
         * @return The current ValidationRule
         */
        public ValidationRule<String> matchPattern(String pattern) {
            if (type != String.class) {
                throw new IllegalArgumentException("Pattern matching only works with String type");
            }
            @SuppressWarnings("unchecked")
            ValidationRule<String> stringRule = (ValidationRule<String>) this;
            stringRule.validators.add(value ->
                    value == null || Pattern.matches(pattern, value)
            );
            return stringRule;
        }

        /**
         * Add a length constraint for string values.
         * @param min Minimum length
         * @param max Maximum length
         * @return The current ValidationRule
         */
        public ValidationRule<String> lengthBetween(int min, int max) {
            if (type != String.class) {
                throw new IllegalArgumentException("Length validation only works with String type");
            }
            @SuppressWarnings("unchecked")
            ValidationRule<String> stringRule = (ValidationRule<String>) this;
            stringRule.validators.add(value ->
                    value == null || (value.length() >= min && value.length() <= max)
            );
            return stringRule;
        }

        /**
         * Add a numeric range constraint for numeric values.
         * @param min Minimum value
         * @param max Maximum value
         * @return The current ValidationRule
         */
        public ValidationRule<Number> rangeBetween(Number min, Number max) {
            if (!Number.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException("Range validation only works with numeric types");
            }
            @SuppressWarnings("unchecked")
            ValidationRule<Number> numberRule = (ValidationRule<Number>) this;
            numberRule.validators.add(value ->
                    value == null || (value.doubleValue() >= min.doubleValue() &&
                            value.doubleValue() <= max.doubleValue())
            );
            return numberRule;
        }

        /**
         * Validate a value against the defined rules.
         * @param cacheValue The value to validate
         * @return Validation result
         */
        public ValidationResult validate(CacheValue cacheValue) {
            if (cacheValue == null || cacheValue.isNull()) {
                if (isRequired) {
                    return ValidationResult.failure(key, "Value is required but was null");
                }
                return ValidationResult.success();
            }

            // Check type compatibility
            T typedValue;
            try {
                switch (type.getSimpleName()) {
                    case "String":
                        if (cacheValue.getType() != CacheValue.Type.STRING) {
                            return ValidationResult.failure(key, "Expected a String type");
                        }
                        typedValue = type.cast(cacheValue.asString());
                        break;
                    case "Integer":
                        if (cacheValue.getType() != CacheValue.Type.INTEGER) {
                            return ValidationResult.failure(key, "Expected an Integer type");
                        }
                        typedValue = type.cast(cacheValue.asInteger());
                        break;
                    case "Boolean":
                        if (cacheValue.getType() != CacheValue.Type.BOOLEAN) {
                            return ValidationResult.failure(key, "Expected a Boolean type");
                        }
                        typedValue = type.cast(cacheValue.asBoolean());
                        break;
                    case "List":
                        if (cacheValue.getType() != CacheValue.Type.LIST) {
                            return ValidationResult.failure(key, "Expected a List type");
                        }
                        typedValue = type.cast(cacheValue.asList());
                        break;
                    default:
                        return ValidationResult.failure(key, "Unsupported type: " + type.getSimpleName());
                }
            } catch (ClassCastException e) {
                return ValidationResult.failure(key, "Type conversion failed: " + e.getMessage());
            }

            // Run custom validators
            for (Predicate<T> validator : validators) {
                if (!validator.test(typedValue)) {
                    return ValidationResult.failure(key, "Failed custom validation");
                }
            }

            return ValidationResult.success();
        }
        /**
         * Checks if the value is compatible with the expected type.
         * Handles boxed and unboxed primitive types.
         */
        private boolean isCompatibleType(Object value) {
            if (type.isInstance(value)) {
                return true;
            }
            // Handle primitive and boxed compatibility
            if (type == Integer.class && value instanceof Integer) {
                return true;
            }
            if (type == Double.class && value instanceof Double) {
                return true;
            }
            if (type == Long.class && value instanceof Long) {
                return true;
            }
            if (type == Float.class && value instanceof Float) {
                return true;
            }
            if (type == Boolean.class && value instanceof Boolean) {
                return true;
            }
            if (type == Byte.class && value instanceof Byte) {
                return true;
            }
            if (type == Character.class && value instanceof Character) {
                return true;
            }
             return false;
        }
    }

    /**
     * Represents the result of a validation operation.
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final String key;
        private final String errorMessage;

        private ValidationResult(boolean isValid, String key, String errorMessage) {
            this.isValid = isValid;
            this.key = key;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult failure(String key, String errorMessage) {
            return new ValidationResult(false, key, errorMessage);
        }

        public boolean isValid() {
            return isValid;
        }

        public String getKey() {
            return key;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Validation manager for CacheBox to apply and manage validation rules.
     */
    public static class ValidationManager {
        private final List<ValidationRule<?>> rules = new ArrayList<>();

        /**
         * Add a validation rule to the manager.
         * @param rule Validation rule to add
         */
        public void addRule(ValidationRule<?> rule) {
            rules.add(rule);
        }

        /**
         * Validate a key-value pair against all registered rules.
         * @param key The key to validate
         * @param value The value to validate
         * @return Validation result
         */
        public ValidationResult validate(String key, CacheValue value) {
            for (ValidationRule<?> rule : rules) {
                if (rule.getKey().equals(key) ){
                    ValidationResult result = rule.validate(value);
                    if (!result.isValid()) {
                        return result;
                    }
                }
            }
            return ValidationResult.success();
        }
    }
}