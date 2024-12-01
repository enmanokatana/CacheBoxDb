package org.athens;

public class CacheQuery {
    private String pattern;
    private Integer minValue;
    private  Integer maxValue;
    private CacheValue.Type typeFilter;

    public static class Builder{
        private String pattern;
        private Integer minValue;
        private  Integer maxValue;
        private CacheValue.Type typeFilter;

        public Builder withPattern(String pattern){
            this.pattern = pattern;
            return this;
        }
        public Builder withRange(Integer min, Integer max) {
            this.minValue = min;
            this.maxValue = max;
            return this;
        }

        public Builder withType(CacheValue.Type type) {
            this.typeFilter = type;
            return this;
        }

        public CacheQuery build() {
            return new CacheQuery(this);
        }
    }
    private CacheQuery(Builder builder) {
        this.pattern = builder.pattern;
        this.minValue = builder.minValue;
        this.maxValue = builder.maxValue;
        this.typeFilter = builder.typeFilter;
    }

    public String getPattern() {
        return pattern;
    }

    public Integer getMinValue() {
        return minValue;
    }

    public Integer getMaxValue() {
        return maxValue;
    }

    public CacheValue.Type getTypeFilter() {
        return typeFilter;
    }
}
