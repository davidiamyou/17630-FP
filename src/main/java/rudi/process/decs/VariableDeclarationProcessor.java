package rudi.process.decs;

import rudi.error.CannotProcessLineException;
import rudi.process.LineProcessor;
import rudi.support.RudiStack;
import rudi.support.RudiUtils;
import rudi.support.variable.VarType;
import rudi.support.variable.Variable;

import static rudi.support.RudiConstant.*;

/**
 * An implementation of {@link LineProcessor} that handles variable declaration.
 */
public class VariableDeclarationProcessor implements LineProcessor {

    private static VariableDeclarationProcessor instance;

    private VariableDeclarationProcessor() {
    }

    public static VariableDeclarationProcessor getInstance() {
        if (null == instance)
            instance = new VariableDeclarationProcessor();
        return instance;
    }

    @Override
    public boolean canProcess(String line) {
        return (line.trim().startsWith(TYPE_INTEGER + SPACE)) ||
                (line.trim().startsWith(TYPE_FLOAT + SPACE)) ||
                (line.trim().startsWith(TYPE_STRING + SPACE));
    }

    @Override
    public void doProcess(int lineNumber, String line) {
        if (!RudiStack.getInstance().peek().isDeclarationMode()) {
            throw new CannotProcessLineException(
                    RudiUtils.resolveGlobalLineNumber(lineNumber),
                    "Misplaced variable declaration: " + line
            );
        }

        line = RudiUtils.stripComments(line).trim();

        VarType type = null;
        if (line.startsWith(TYPE_INTEGER + SPACE)) {
            type = VarType.INTEGER;
        } else if (line.startsWith(TYPE_FLOAT + SPACE)) {
            type = VarType.FLOAT;
        } else if (line.startsWith(TYPE_STRING + SPACE)) {
            type = VarType.STRING;
        } else {
            throw new CannotProcessLineException(
                    RudiUtils.resolveGlobalLineNumber(lineNumber),
                    "Unknown data type: " + line
            );
        }

        String variableName = null;
        if (line.startsWith(TYPE_INTEGER + SPACE)) {
            variableName = line.substring((TYPE_INTEGER + SPACE).length()).trim();
        } else if (line.startsWith(TYPE_FLOAT + SPACE)) {
            variableName = line.substring((TYPE_FLOAT + SPACE).length()).trim();
        } else if (line.startsWith(TYPE_STRING + SPACE)) {
            variableName = line.substring((TYPE_STRING + SPACE).length()).trim();
        }

        // TODO
        // maybe do some additional checks for variable name formats
        // i.e. a-zA-Z0-9 and do not start with number

        RudiStack.getInstance().peek().declare(new Variable(type, variableName));
    }
}