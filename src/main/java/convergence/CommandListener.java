// Generated from /home/nicholasdelello/IntelliJIDEAProjects/convergenceBot/src/main/kotlin/convergence/Command.g4 by ANTLR 4.13.2
package convergence;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link CommandParser}.
 */
public interface CommandListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link CommandParser#command}.
	 * @param ctx the parse tree
	 */
	void enterCommand(CommandParser.CommandContext ctx);
	/**
	 * Exit a parse tree produced by {@link CommandParser#command}.
	 * @param ctx the parse tree
	 */
	void exitCommand(CommandParser.CommandContext ctx);
	/**
	 * Enter a parse tree produced by {@link CommandParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterArgument(CommandParser.ArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link CommandParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitArgument(CommandParser.ArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link CommandParser#nonQuoteArgument}.
	 * @param ctx the parse tree
	 */
	void enterNonQuoteArgument(CommandParser.NonQuoteArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link CommandParser#nonQuoteArgument}.
	 * @param ctx the parse tree
	 */
	void exitNonQuoteArgument(CommandParser.NonQuoteArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link CommandParser#quoteArgument}.
	 * @param ctx the parse tree
	 */
	void enterQuoteArgument(CommandParser.QuoteArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link CommandParser#quoteArgument}.
	 * @param ctx the parse tree
	 */
	void exitQuoteArgument(CommandParser.QuoteArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link CommandParser#notQuote}.
	 * @param ctx the parse tree
	 */
	void enterNotQuote(CommandParser.NotQuoteContext ctx);
	/**
	 * Exit a parse tree produced by {@link CommandParser#notQuote}.
	 * @param ctx the parse tree
	 */
	void exitNotQuote(CommandParser.NotQuoteContext ctx);
	/**
	 * Enter a parse tree produced by {@link CommandParser#commandName}.
	 * @param ctx the parse tree
	 */
	void enterCommandName(CommandParser.CommandNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CommandParser#commandName}.
	 * @param ctx the parse tree
	 */
	void exitCommandName(CommandParser.CommandNameContext ctx);
}