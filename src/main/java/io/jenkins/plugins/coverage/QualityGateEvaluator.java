package io.jenkins.plugins.coverage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.errorprone.annotations.FormatMethod;

import io.jenkins.plugins.coverage.model.CoverageNode;

/**
 *
 * @author Nikolas Paripovic / Michael Müller
 */
public class QualityGateEvaluator {
    private final List<QualityGate> qualityGates = new ArrayList<>();

    /**
     *
     * @param coverageNode
     * @param qualityGates
     * @param logger
     * @return
     */
    public QualityGateStatus evaluate(final CoverageNode coverageNode, final List<QualityGate> qualityGates, final FormattedLogger logger) {
        return null;
    }

    /**
     * Logs results of the quality gate evaluation.
     */
    @FunctionalInterface
    public interface FormattedLogger {
        /**
         * Logs the specified message.
         *
         * @param format
         *         A <a href="../util/Formatter.html#syntax">format string</a>
         * @param args
         *         Arguments referenced by the format specifiers in the format string.  If there are more arguments than
         *         format specifiers, the extra arguments are ignored.  The number of arguments is variable and may be
         *         zero.
         */
        @FormatMethod
        void print(String format, Object... args);
    }
}
