// Generated from /home/nicholasdelello/IntelliJIDEAProjects/convergenceBot/src/main/kotlin/convergence/command.g4 by ANTLR 4.13.1
package convergence;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link commandParser}.
 */
public interface commandListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link commandParser#command}.
	 * @param ctx the parse tree
	 */
	void enterCommand(commandParser.CommandContext ctx);
	/**
	 * Exit a parse tree produced by {@link commandParser#command}.
	 * @param ctx the parse tree
	 */
	void exitCommand(commandParser.CommandContext ctx);
	/**
	 * Enter a parse tree produced by {@link commandParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterArgument(commandParser.ArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link commandParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitArgument(commandParser.ArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link commandParser#nonQuoteArgument}.
	 * @param ctx the parse tree
	 */
	void enterNonQuoteArgument(commandParser.NonQuoteArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link commandParser#nonQuoteArgument}.
	 * @param ctx the parse tree
	 */
	void exitNonQuoteArgument(commandParser.NonQuoteArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link commandParser#quoteArgument}.
	 * @param ctx the parse tree
	 */
	void enterQuoteArgument(commandParser.QuoteArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link commandParser#quoteArgument}.
	 * @param ctx the parse tree
	 */
	void exitQuoteArgument(commandParser.QuoteArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link commandParser#notQuote}.
	 * @param ctx the parse tree
	 */
	void enterNotQuote(commandParser.NotQuoteContext ctx);
	/**
	 * Exit a parse tree produced by {@link commandParser#notQuote}.
	 * @param ctx the parse tree
	 */
	void exitNotQuote(commandParser.NotQuoteContext ctx);
	/**
	 * Enter a parse tree produced by {@link commandParser#commandName}.
	 * @param ctx the parse tree
	 */
	void enterCommandName(commandParser.CommandNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link commandParser#commandName}.
	 * @param ctx the parse tree
	 */
	void exitCommandName(commandParser.CommandNameContext ctx);
}