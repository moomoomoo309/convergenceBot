// Generated from /home/nicholasdelello/IntelliJIDEAProjects/convergenceBot/src/main/kotlin/convergence/command.g4 by ANTLR 4.13.1
package convergence;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link CommandParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 *            operations with no return type.
 */
public interface CommandVisitor<T> extends ParseTreeVisitor<T> {
    /**
     * Visit a parse tree produced by {@link CommandParser#command}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitCommand(CommandParser.CommandContext ctx);

    /**
     * Visit a parse tree produced by {@link CommandParser#argument}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitArgument(CommandParser.ArgumentContext ctx);

    /**
     * Visit a parse tree produced by {@link CommandParser#nonQuoteArgument}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitNonQuoteArgument(CommandParser.NonQuoteArgumentContext ctx);

    /**
     * Visit a parse tree produced by {@link CommandParser#quoteArgument}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitQuoteArgument(CommandParser.QuoteArgumentContext ctx);

    /**
     * Visit a parse tree produced by {@link CommandParser#notQuote}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitNotQuote(CommandParser.NotQuoteContext ctx);

    /**
     * Visit a parse tree produced by {@link CommandParser#commandName}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitCommandName(CommandParser.CommandNameContext ctx);
}
