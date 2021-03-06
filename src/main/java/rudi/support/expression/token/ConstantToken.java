package rudi.support.expression.token;

import rudi.support.expression.eval.Evaluator;
import rudi.support.literal.Constant;

/**
 * A constant token in expression
 */
public class ConstantToken extends Token {

    private final Constant constant;

    public ConstantToken(String faceValue, Constant constant) {
        super(faceValue);
        this.constant = constant;
    }

    public Constant getConstant() {
        return constant;
    }

    @Override
    public boolean isOperand() {
        return true;
    }

    @Override
    public boolean isOperator() {
        return false;
    }

    @Override
    public boolean isParenthesis() {
        return false;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public Evaluator evaluator() {
        return ((lhs, rhs) -> this);
    }
}
