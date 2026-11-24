package com.ledgerguard.analytics;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
@AnalyticsQueryParams.ValidAnalyticsWindow
public class AnalyticsQueryParams {
    private String from;
    private String to;
    @Pattern(regexp = "day|week|month", message = "granularity must be one of day, week, month")
    private String granularity = "day";
    @Size(max = 8, message = "at most 8 resources are allowed")
    private List<@Pattern(regexp = "^[A-Z_]{1,32}$", message = "resource must match ^[A-Z_]{1,32}$") String> resource;
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    public String getGranularity() { return granularity; }
    public void setGranularity(String granularity) { this.granularity = granularity; }
    public List<String> getResource() { return resource; }
    public void setResource(List<String> resource) { this.resource = resource; }
    @Documented
    @Constraint(validatedBy = AnalyticsWindowValidator.class)
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ValidAnalyticsWindow {
        String message() default "Invalid analytics date window";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }
    public static class AnalyticsWindowValidator implements ConstraintValidator<ValidAnalyticsWindow, AnalyticsQueryParams> {
        private static final DateTimeFormatter STRICT_ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE
                .withResolverStyle(ResolverStyle.STRICT);
        private final Clock clock;
        public AnalyticsWindowValidator(Clock clock) {
            this.clock = clock;
        }
        @Override
        public boolean isValid(AnalyticsQueryParams params, ConstraintValidatorContext context) {
            if (params == null) {
                return true;
            }
            LocalDate todayUtc = LocalDate.now(clock);
            LocalDate parsedTo;
            if (params.getTo() == null || params.getTo().isBlank()) {
                parsedTo = todayUtc;
            } else {
                parsedTo = parseStrictDate(params.getTo(), "to", context);
                if (parsedTo == null) {
                    return false;
                }
            }
            if (parsedTo.isAfter(todayUtc)) {
                addViolation(context, "to", "to date must not be in the future");
                return false;
            }
            LocalDate parsedFrom;
            if (params.getFrom() == null || params.getFrom().isBlank()) {
                parsedFrom = parsedTo.minusDays(29);
            } else {
                parsedFrom = parseStrictDate(params.getFrom(), "from", context);
                if (parsedFrom == null) {
                    return false;
                }
            }
            if (parsedFrom.isAfter(parsedTo)) {
                addViolation(context, "from", "from date must be on or before to date");
                return false;
            }
            long spanDays = ChronoUnit.DAYS.between(parsedFrom, parsedTo) + 1;
            String gran = params.getGranularity() == null ? "day" : params.getGranularity();
            long maxDays = switch (gran) {
                case "week" -> 730L;
                case "month" -> 1826L;
                default -> 366L;
            };
            if (spanDays > maxDays) {
                addViolation(context, "from", "date range exceeds " + maxDays + " days for granularity " + gran);
                return false;
            }
            return true;
        }
        private LocalDate parseStrictDate(String raw, String field, ConstraintValidatorContext context) {
            try {
                return LocalDate.parse(raw, STRICT_ISO_DATE);
            } catch (DateTimeParseException ex) {
                addViolation(context, field, "invalid calendar date: " + raw);
                return null;
            }
        }
        private void addViolation(ConstraintValidatorContext context, String field, String message) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode(field)
                    .addConstraintViolation();
        }
    }
}