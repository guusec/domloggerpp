package burp.parser;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Parser for DOMLogger++ search query syntax
 * Supports: sink.field.operator:"value" with AND/OR logic
 */
public class QueryParser {

    private static final Map<String, String> COLUMN_MAPPING = new HashMap<>();

    static {
        COLUMN_MAPPING.put("sink.id", "id");
        COLUMN_MAPPING.put("sink.dupKey", "dupKey");
        COLUMN_MAPPING.put("sink.debug", "debug");
        COLUMN_MAPPING.put("sink.aiScore", "aiScore");
        COLUMN_MAPPING.put("sink.alert", "alert");
        COLUMN_MAPPING.put("sink.tag", "tag");
        COLUMN_MAPPING.put("sink.type", "type");
        COLUMN_MAPPING.put("sink.date", "date");
        COLUMN_MAPPING.put("sink.href", "href");
        COLUMN_MAPPING.put("sink.frame", "frame");
        COLUMN_MAPPING.put("sink.sink", "sink");
        COLUMN_MAPPING.put("sink.data", "data");
        COLUMN_MAPPING.put("sink.trace", "trace");
        COLUMN_MAPPING.put("sink.favorite", "favorite");
    }

    /**
     * Parse condition string and convert to SQL
     */
    public static SqlResult conditionToSql(String condition) throws ParseException {
        if (condition == null || condition.trim().isEmpty()) {
            return new SqlResult("", new ArrayList<>());
        }

        List<Token> tokens = tokenize(condition);
        AstNode ast = parseExpression(tokens);
        return astToSql(ast);
    }

    /**
     * Tokenize the condition string
     */
    private static List<Token> tokenize(String condition) throws ParseException {
        List<Token> tokens = new ArrayList<>();
        int i = 0;

        while (i < condition.length()) {
            char ch = condition.charAt(i);

            // Skip whitespace
            if (Character.isWhitespace(ch)) {
                i++;
                continue;
            }

            // Handle parentheses
            if (ch == '(' || ch == ')') {
                tokens.add(new Token(TokenType.PAREN, String.valueOf(ch)));
                i++;
                continue;
            }

            // Handle quoted strings
            if (ch == '"') {
                StringBuilder value = new StringBuilder();
                i++; // Skip opening quote

                while (i < condition.length()) {
                    char current = condition.charAt(i);

                    if (current == '"') {
                        // Check if escaped
                        if (i > 0 && condition.charAt(i - 1) == '\\') {
                            value.append('"');
                            i++;
                        } else {
                            // Closing quote
                            i++;
                            break;
                        }
                    } else if (current == '\\' && i + 1 < condition.length() && condition.charAt(i + 1) == '"') {
                        value.append('"');
                        i += 2;
                    } else {
                        value.append(current);
                        i++;
                    }
                }

                tokens.add(new Token(TokenType.STRING, value.toString()));
                continue;
            }

            // Handle colon
            if (ch == ':') {
                tokens.add(new Token(TokenType.COLON, ":"));
                i++;
                continue;
            }

            // Handle identifiers and operators
            if (Character.isLetterOrDigit(ch) || ch == '.' || ch == '_') {
                StringBuilder value = new StringBuilder();
                while (i < condition.length()) {
                    char current = condition.charAt(i);
                    if (Character.isLetterOrDigit(current) || current == '.' || current == '_') {
                        value.append(current);
                        i++;
                    } else {
                        break;
                    }
                }

                String identifier = value.toString();

                // Check if logical operator
                if ("AND".equals(identifier) || "OR".equals(identifier)) {
                    tokens.add(new Token(TokenType.LOGICAL, identifier));
                } else {
                    // Check if comparison expression (object.property.operator)
                    String[] parts = identifier.split("\\.");
                    if (parts.length >= 3) {
                        // Reconstruct object path and operator
                        String objectPath = String.join(".", Arrays.copyOfRange(parts, 0, parts.length - 1));
                        String operator = parts[parts.length - 1];

                        tokens.add(new Token(TokenType.IDENTIFIER, objectPath));
                        tokens.add(new Token(TokenType.COLON, ":"));
                        tokens.add(new Token(TokenType.IDENTIFIER, operator));
                    } else {
                        tokens.add(new Token(TokenType.IDENTIFIER, identifier));
                    }
                }
                continue;
            }

            i++;
        }

        return tokens;
    }

    /**
     * Parse tokens into AST
     */
    private static AstNode parseExpression(List<Token> tokens) throws ParseException {
        TokenIterator iterator = new TokenIterator(tokens);
        return parseOr(iterator);
    }

    private static AstNode parseOr(TokenIterator iterator) throws ParseException {
        AstNode left = parseAnd(iterator);

        while (iterator.hasNext() && iterator.peek().type == TokenType.LOGICAL &&
               "OR".equals(iterator.peek().value)) {
            iterator.next(); // consume OR
            AstNode right = parseAnd(iterator);
            left = new LogicalNode("or", left, right);
        }

        return left;
    }

    private static AstNode parseAnd(TokenIterator iterator) throws ParseException {
        AstNode left = parsePrimary(iterator);

        while (iterator.hasNext() && iterator.peek().type == TokenType.LOGICAL &&
               "AND".equals(iterator.peek().value)) {
            iterator.next(); // consume AND
            AstNode right = parsePrimary(iterator);
            left = new LogicalNode("and", left, right);
        }

        return left;
    }

    private static AstNode parsePrimary(TokenIterator iterator) throws ParseException {
        if (!iterator.hasNext()) {
            throw new ParseException("Unexpected end of expression");
        }

        Token token = iterator.peek();

        if (token.type == TokenType.PAREN && "(".equals(token.value)) {
            iterator.next(); // consume (
            AstNode expr = parseOr(iterator);

            if (!iterator.hasNext() || iterator.peek().type != TokenType.PAREN ||
                !")".equals(iterator.peek().value)) {
                throw new ParseException("Expected closing parenthesis");
            }

            iterator.next(); // consume )
            return expr;
        }

        if (token.type == TokenType.IDENTIFIER) {
            return parseComparison(iterator);
        }

        throw new ParseException("Unexpected token: " + token.type);
    }

    private static ComparisonNode parseComparison(TokenIterator iterator) throws ParseException {
        if (!iterator.hasNext()) {
            throw new ParseException("Expected object path");
        }

        String objectPath = iterator.next().value;

        if (!iterator.hasNext() || iterator.peek().type != TokenType.COLON) {
            throw new ParseException("Expected colon after object path");
        }
        iterator.next(); // consume :

        if (!iterator.hasNext() || iterator.peek().type != TokenType.IDENTIFIER) {
            throw new ParseException("Expected operator");
        }
        String operator = iterator.next().value;

        if (!iterator.hasNext() || iterator.peek().type != TokenType.COLON) {
            throw new ParseException("Expected colon after operator");
        }
        iterator.next(); // consume :

        if (!iterator.hasNext() || iterator.peek().type != TokenType.STRING) {
            throw new ParseException("Expected string value");
        }
        String value = iterator.next().value;

        return new ComparisonNode(objectPath, operator, value);
    }

    /**
     * Convert AST to SQL
     */
    private static SqlResult astToSql(AstNode ast) throws ParseException {
        if (ast instanceof ComparisonNode) {
            return comparisonToSql((ComparisonNode) ast);
        } else if (ast instanceof LogicalNode) {
            LogicalNode logical = (LogicalNode) ast;
            SqlResult left = astToSql(logical.left);
            SqlResult right = astToSql(logical.right);

            String whereClause = String.format("(%s %s %s)",
                left.whereClause,
                logical.operator.toUpperCase(),
                right.whereClause);

            List<String> params = new ArrayList<>();
            params.addAll(left.params);
            params.addAll(right.params);

            return new SqlResult(whereClause, params);
        }

        throw new ParseException("Unknown AST node type");
    }

    private static SqlResult comparisonToSql(ComparisonNode node) throws ParseException {
        String column = COLUMN_MAPPING.get(node.objectPath);
        if (column == null) {
            throw new ParseException("Invalid column path: " + node.objectPath);
        }

        switch (node.operator) {
            case "eq":
                return new SqlResult(column + " = ?", Arrays.asList(node.value));

            case "ne":
            case "neq":
                return new SqlResult(column + " != ?", Arrays.asList(node.value));

            case "like":
                return new SqlResult(column + " LIKE ?", Arrays.asList(node.value));

            case "nlike":
                return new SqlResult(column + " NOT LIKE ?", Arrays.asList(node.value));

            case "cont":
                return new SqlResult(column + " LIKE ?", Arrays.asList("%" + node.value + "%"));

            case "ncont":
                return new SqlResult(column + " NOT LIKE ?", Arrays.asList("%" + node.value + "%"));

            default:
                throw new ParseException("Unknown operator: " + node.operator);
        }
    }

    // Helper classes
    private static class TokenIterator {
        private final List<Token> tokens;
        private int index = 0;

        TokenIterator(List<Token> tokens) {
            this.tokens = tokens;
        }

        boolean hasNext() {
            return index < tokens.size();
        }

        Token peek() {
            return tokens.get(index);
        }

        Token next() {
            return tokens.get(index++);
        }
    }

    // Inner classes
    public static class Token {
        TokenType type;
        String value;

        Token(TokenType type, String value) {
            this.type = type;
            this.value = value;
        }
    }

    public enum TokenType {
        PAREN, STRING, IDENTIFIER, LOGICAL, COLON
    }

    public interface AstNode {}

    public static class ComparisonNode implements AstNode {
        String objectPath;
        String operator;
        String value;

        ComparisonNode(String objectPath, String operator, String value) {
            this.objectPath = objectPath;
            this.operator = operator;
            this.value = value;
        }
    }

    public static class LogicalNode implements AstNode {
        String operator;
        AstNode left;
        AstNode right;

        LogicalNode(String operator, AstNode left, AstNode right) {
            this.operator = operator;
            this.left = left;
            this.right = right;
        }
    }

    public static class SqlResult {
        public final String whereClause;
        public final List<String> params;

        public SqlResult(String whereClause, List<String> params) {
            this.whereClause = whereClause;
            this.params = params;
        }
    }

    public static class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }
    }
}
