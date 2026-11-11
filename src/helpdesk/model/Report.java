package helpdesk.model;

/**
 * One of the Week 4 report queries.
 * If parameterPrompt is not null, the SQL has one integer "?" the user must fill in.
 */
public record Report(int number, String question, String sqlFeature, String sql, String parameterPrompt) {

    public boolean needsParameter() {
        return parameterPrompt != null;
    }
}
