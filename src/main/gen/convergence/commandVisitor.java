// Generated from /home/nicholasdelello/IntelliJIDEAProjects/convergenceBot/src/main/kotlin/convergence/command.g4 by ANTLR 4.8
package convergence;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link commandParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 *            operations with no return type.
 */
public interface commandVisitor<T> extends ParseTreeVisitor<T> {
    /**
     * Visit a parse tree produced by {@link commandParser#command}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitCommand(commandParser.CommandContext ctx);

    /**
     * Visit a parse tree produced by {@link commandParser#argument}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitArgument(commandParser.ArgumentContext ctx);

    /**
     * Visit a parse tree produced by {@link commandParser#nonQuoteArgument}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitNonQuoteArgument(commandParser.NonQuoteArgumentContext ctx);

    /**
     * Visit a parse tree produced by {@link commandParser#quoteArgument}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitQuoteArgument(commandParser.QuoteArgumentContext ctx);

    /**
     * Visit a parse tree produced by {@link commandParser#notQuote}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitNotQuote(commandParser.NotQuoteContext ctx);

    /**
     * Visit a parse tree produced by {@link commandParser#commandName}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitCommandName(commandParser.CommandNameContext ctx);
}