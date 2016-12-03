package rudi.process.pre;

import rudi.error.CannotProcessLineException;
import rudi.support.RudiConstant;
import rudi.support.RudiSource;
import rudi.support.RudiSourceRegistry;
import rudi.support.RudiUtils;

/**
 * A preprocessor for the source code responsible for
 * 1. strip out all comments
 * 2. concat line continuation together
 * 3. parse source into {@link rudi.support.RudiSourceRegistry}
 */
public class SourcePreProcessor {

    private final RudiSource source;
    private boolean commentMode = false;
    private int continuationFirstLineNumber = 0;
    private String continuationSourceBuffer = "";
    private String currentRoutineName = "";
    private int currentRoutineStartLineNumber = 0;
    private int currentRoutineEndLineNumber = 0;

    public static RudiSource process(RudiSource source) {
        SourcePreProcessor p = new SourcePreProcessor(source);
        p.doProcess();
        return p.source;
    }

    private SourcePreProcessor(final RudiSource source) {
        this.source = source.clone();
    }

    private void doProcess() {
        for (int i = 1; i <= this.source.totalLines(); i++) {
            this.doProcessSingleLine(i, this.source.getLine(i));
        }
        if (this.commentMode) {
            throw new CannotProcessLineException(
                    this.source.totalLines(),
                    "Comment is not closed properly.");
        } else if (this.continuationSourceBuffer.length() > 0) {
            throw new CannotProcessLineException(
                    this.source.totalLines(),
                    "Line continuation is not terminated properly.");
        } else if (this.currentRoutineName.length() > 0) {
            throw new CannotProcessLineException(
                    this.source.totalLines(),
                    "Routine declaration is not terminated properly.");
        }
    }

    private void doProcessSingleLine(int lineNumber, String line) {
        // get rid of inline comments and wrapping spaces, if any
        line = RudiUtils.stripComments(line).trim();
        updateLine(lineNumber, line);

        // deal with block comments
        if (line.length() > 0) {
            if (line.startsWith(RudiConstant.START_COMMENT)) {
                this.commentMode = true;
                clearLine(lineNumber);
            }
            if (this.commentMode) {
                clearLine(lineNumber);
            }
            if (line.endsWith(RudiConstant.END_COMMENT)) {
                clearLine(lineNumber);
                this.commentMode = false;
            }
        }

        // deal with brackets (check)
        if (line.length() > 0) {
            if (line.endsWith(RudiConstant.START_BRAC) && !line.equals(RudiConstant.START_BRAC)) {
                throw new CannotProcessLineException(lineNumber, "Start and end bracket should be on its own line");
            }
            if (line.startsWith(RudiConstant.END_BRAC) && !line.equals(RudiConstant.END_BRAC)) {
                throw new CannotProcessLineException(lineNumber, "Start and end bracket should be on its own line");
            }
        }

        // deal with continuation
        if (line.length() > 0) {
            if (line.endsWith(RudiConstant.SPACE + RudiConstant.CONTINUATION)) {
                if (continuationFirstLineNumber == 0) {
                    continuationFirstLineNumber = lineNumber;
                }
                continuationSourceBuffer = (continuationSourceBuffer
                        + RudiConstant.SPACE
                        + line.substring(0, line.length() - 1)).trim();
                clearLine(lineNumber);
            } else {
                if (continuationSourceBuffer.length() > 0) {
                    continuationSourceBuffer = (continuationSourceBuffer
                            + RudiConstant.SPACE
                            + line).trim();
                    updateLine(continuationFirstLineNumber, continuationSourceBuffer);
                    clearLine(lineNumber);
                    continuationSourceBuffer = "";
                    continuationFirstLineNumber = 0;
                }
            }
        }

        // deal with parsing the source
        if (RudiConstant.PROGRAM_COMMAND.equals(line.toLowerCase())) {
            if (currentRoutineName.length() == 0) {
                currentRoutineName = RudiConstant.MAIN_PROGRAM_KEY;
                currentRoutineStartLineNumber = lineNumber;
            } else {
                throw new CannotProcessLineException(lineNumber, "Cannot embed main function in other routines.");
            }
        } else if (line.toLowerCase().startsWith(RudiConstant.SUBROUTINE_COMMAND)) {
            if (currentRoutineName.length() == 0) {
                try {
                    currentRoutineName = line.substring(
                            RudiConstant.SUBROUTINE_COMMAND.length(),
                            line.indexOf(RudiConstant.LEFT_PAREN)).trim();
                    currentRoutineStartLineNumber = lineNumber;
                } catch (StringIndexOutOfBoundsException e) {
                    throw new CannotProcessLineException(lineNumber, "Unrecognized name for subroutine");
                }
            } else {
                throw new CannotProcessLineException(lineNumber, "Cannot embed subroutine declaration in other routines.");
            }
        } else if (RudiConstant.END_COMMAND.equals(line.toLowerCase())) {
            if (currentRoutineName.length() == 0) {
                throw new CannotProcessLineException(lineNumber, "Cannot end main routine that was not started");
            } else if (!currentRoutineName.equals(RudiConstant.MAIN_PROGRAM_KEY)) {
                throw new CannotProcessLineException(lineNumber, "Cannot end subroutine with 'end'.");
            } else {
                currentRoutineEndLineNumber = lineNumber;
                RudiSourceRegistry.getInstance().put(
                        currentRoutineName,
                        new RudiSource(this.source, currentRoutineStartLineNumber, currentRoutineEndLineNumber));
                currentRoutineName = "";
                currentRoutineStartLineNumber = 0;
                currentRoutineEndLineNumber = 0;
            }
        } else if (RudiConstant.RETURN_COMMAND.equals(line.toLowerCase())) {
            if (currentRoutineName.length() == 0) {
                throw new CannotProcessLineException(lineNumber, "Cannot end subroutine that was not started");
            } else if (currentRoutineName.equals(RudiConstant.MAIN_PROGRAM_KEY)) {
                throw new CannotProcessLineException(lineNumber, "Cannot end main routine with 'return'.");
            } else {
                currentRoutineEndLineNumber = lineNumber;
                RudiSourceRegistry.getInstance().put(
                        currentRoutineName,
                        new RudiSource(this.source, currentRoutineStartLineNumber, currentRoutineEndLineNumber));
                currentRoutineName = "";
                currentRoutineStartLineNumber = 0;
                currentRoutineEndLineNumber = 0;
            }
        }
    }

    private void updateLine(int lineNumber, String content) {
        this.source.updateLine(lineNumber, content);
    }

    private void clearLine(int lineNumber) {
        updateLine(lineNumber, "");
    }
}