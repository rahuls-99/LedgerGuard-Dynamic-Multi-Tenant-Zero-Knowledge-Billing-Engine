package com.ledgerguard.analytics;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
class AnalyticsQueryParamsTest {
    private static ValidatorFactory factory;
    private static Validator validator;
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneOffset.UTC);
    @BeforeAll
    static void setUp() {
        factory = Validation.byDefaultProvider()
                .configure()
                .constraintValidatorFactory(new jakarta.validation.ConstraintValidatorFactory() {
                    @Override
                    public <T extends jakarta.validation.ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                        if (key == AnalyticsQueryParams.AnalyticsWindowValidator.class) {
                            return key.cast(new AnalyticsQueryParams.AnalyticsWindowValidator(FIXED_CLOCK));
                        }
                        try {
                            return key.getDeclaredConstructor().newInstance();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    @Override
                    public void releaseInstance(jakarta.validation.ConstraintValidator<?, ?> instance) {
                    }
                })
                .buildValidatorFactory();
        validator = factory.getValidator();
    }
    @AfterAll
    static void tearDown() {
        factory.close();
    }
    @Test
    void acceptsValidBoundaryAndDefaults() {
        AnalyticsQueryParams p = new AnalyticsQueryParams();
        p.setFrom("2025-11-21"); // 366 days inclusive ending 2026-11-20
        p.setTo("2026-11-20");
        p.setGranularity("day");
        p.setResource(List.of("API_CALLS", "STORAGE_GB"));
        Set<ConstraintViolation<AnalyticsQueryParams>> violations = validator.validate(p);
        assertTrue(violations.isEmpty());
    }
    @Test
    void rejectsInvalidCalendarDateFeb30() {
        AnalyticsQueryParams p = new AnalyticsQueryParams();
        p.setFrom("2026-02-30");
        p.setTo("2026-03-01");
        Set<ConstraintViolation<AnalyticsQueryParams>> violations = validator.validate(p);
        assertFalse(violations.isEmpty());
    }
    @Test
    void rejectsFromAfterToAndFutureTo() {
        AnalyticsQueryParams reverse = new AnalyticsQueryParams();
        reverse.setFrom("2026-11-21");
        reverse.setTo("2026-11-20");
        assertFalse(validator.validate(reverse).isEmpty());
        AnalyticsQueryParams future = new AnalyticsQueryParams();
        future.setFrom("2026-11-20");
        future.setTo(LocalDate.now(FIXED_CLOCK).plusDays(1).toString());
        assertFalse(validator.validate(future).isEmpty());
    }
    @Test
    void rejectsOverRangeAndNineResourcesOrBadPattern() {
        AnalyticsQueryParams overDay = new AnalyticsQueryParams();
        overDay.setFrom("2025-11-19"); // 367 days inclusive
        overDay.setTo("2026-11-20");
        overDay.setGranularity("day");
        assertFalse(validator.validate(overDay).isEmpty());
        AnalyticsQueryParams nineResources = new AnalyticsQueryParams();
        nineResources.setResource(List.of("R1", "R2", "R3", "R4", "R5", "R6", "R7", "R8", "R9"));
        assertFalse(validator.validate(nineResources).isEmpty());
        AnalyticsQueryParams badPattern = new AnalyticsQueryParams();
        badPattern.setResource(List.of("api_calls"));
        assertFalse(validator.validate(badPattern).isEmpty());
    }
}