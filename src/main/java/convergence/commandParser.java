// Generated from /home/nicholasdelello/IntelliJIDEAProjects/convergenceBot/src/main/kotlin/convergence/command.g4 by ANTLR 4.9.1
package convergence;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class commandParser extends Parser {
	public static final int
			RegularEscape = 1, UnicodeEscape = 2, OctalEscape = 3, InvalidEscape = 4, Alnum = 5,
			Whitespace = 6, NotWhitespaceOrQuote = 7, Quote = 8;
	public static final int
			RULE_command = 0, RULE_argument = 1, RULE_nonQuoteArgument = 2, RULE_quoteArgument = 3,
			RULE_notQuote = 4, RULE_commandName = 5;
	public static final String[] ruleNames = makeRuleNames();
	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	public static final String _serializedATN =
			"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\n?\4\2\t\2\4\3\t" +
					"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\3\2\3\2\7\2\21\n\2\f\2\16\2\24\13\2" +
					"\3\2\3\2\6\2\30\n\2\r\2\16\2\31\3\2\5\2\35\n\2\7\2\37\n\2\f\2\16\2\"\13" +
					"\2\3\2\3\2\3\3\3\3\5\3(\n\3\3\4\6\4+\n\4\r\4\16\4,\3\5\3\5\7\5\61\n\5" +
					"\f\5\16\5\64\13\5\3\5\3\5\3\6\3\6\3\7\6\7;\n\7\r\7\16\7<\3\7\2\2\b\2\4" +
					"\6\b\n\f\2\4\4\2\3\7\t\t\3\2\3\t\2@\2\16\3\2\2\2\4\'\3\2\2\2\6*\3\2\2" +
					"\2\b.\3\2\2\2\n\67\3\2\2\2\f:\3\2\2\2\16\22\5\f\7\2\17\21\7\b\2\2\20\17" +
					"\3\2\2\2\21\24\3\2\2\2\22\20\3\2\2\2\22\23\3\2\2\2\23 \3\2\2\2\24\22\3" +
					"\2\2\2\25\34\5\4\3\2\26\30\7\b\2\2\27\26\3\2\2\2\30\31\3\2\2\2\31\27\3" +
					"\2\2\2\31\32\3\2\2\2\32\35\3\2\2\2\33\35\7\2\2\3\34\27\3\2\2\2\34\33\3" +
					"\2\2\2\35\37\3\2\2\2\36\25\3\2\2\2\37\"\3\2\2\2 \36\3\2\2\2 !\3\2\2\2" +
					"!#\3\2\2\2\" \3\2\2\2#$\7\2\2\3$\3\3\2\2\2%(\5\b\5\2&(\5\6\4\2\'%\3\2" +
					"\2\2\'&\3\2\2\2(\5\3\2\2\2)+\t\2\2\2*)\3\2\2\2+,\3\2\2\2,*\3\2\2\2,-\3" +
					"\2\2\2-\7\3\2\2\2.\62\7\n\2\2/\61\5\n\6\2\60/\3\2\2\2\61\64\3\2\2\2\62" +
					"\60\3\2\2\2\62\63\3\2\2\2\63\65\3\2\2\2\64\62\3\2\2\2\65\66\7\n\2\2\66" +
					"\t\3\2\2\2\678\t\3\2\28\13\3\2\2\29;\7\7\2\2:9\3\2\2\2;<\3\2\2\2<:\3\2" +
					"\2\2<=\3\2\2\2=\r\3\2\2\2\n\22\31\34 \',\62<";
	public static final ATN _ATN =
			new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
			new PredictionContextCache();
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	static {
		RuntimeMetaData.checkVersion("4.9.1", RuntimeMetaData.VERSION);
	}

	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}

	public commandParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
	}

	private static String[] makeRuleNames() {
		return new String[]{
				"command", "argument", "nonQuoteArgument", "quoteArgument", "notQuote",
				"commandName"
		};
	}

	private static String[] makeLiteralNames() {
		return new String[]{
				null, null, null, null, null, null, null, null, "'\"'"
		};
	}

	private static String[] makeSymbolicNames() {
		return new String[]{
				null, "RegularEscape", "UnicodeEscape", "OctalEscape", "InvalidEscape",
				"Alnum", "Whitespace", "NotWhitespaceOrQuote", "Quote"
		};
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() {
		return "command.g4";
	}

	@Override
	public String[] getRuleNames() {
		return ruleNames;
	}

	@Override
	public String getSerializedATN() {
		return _serializedATN;
	}

	@Override
	public ATN getATN() {
		return _ATN;
	}

	public final CommandContext command() throws RecognitionException {
		CommandContext _localctx = new CommandContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_command);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
				setState(12);
				commandName();
				setState(16);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la == Whitespace) {
					{
						{
							setState(13);
							match(Whitespace);
						}
					}
					setState(18);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(30);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << RegularEscape) | (1L << UnicodeEscape) | (1L << OctalEscape) | (1L << InvalidEscape) | (1L << Alnum) | (1L << NotWhitespaceOrQuote) | (1L << Quote))) != 0)) {
					{
						{
							setState(19);
							argument();
							setState(26);
							_errHandler.sync(this);
							switch (_input.LA(1)) {
								case Whitespace: {
									setState(21);
									_errHandler.sync(this);
									_la = _input.LA(1);
									do {
										{
											{
												setState(20);
												match(Whitespace);
											}
										}
										setState(23);
										_errHandler.sync(this);
										_la = _input.LA(1);
									} while (_la == Whitespace);
								}
								break;
								case EOF: {
									setState(25);
									match(EOF);
								}
								break;
								default:
									throw new NoViableAltException(this);
							}
						}
					}
					setState(32);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(33);
				match(EOF);
			}
		} catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		} finally {
			exitRule();
		}
		return _localctx;
	}

	public final ArgumentContext argument() throws RecognitionException {
		ArgumentContext _localctx = new ArgumentContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_argument);
		try {
			setState(37);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
				case Quote:
					enterOuterAlt(_localctx, 1);
				{
					setState(35);
					quoteArgument();
				}
				break;
				case RegularEscape:
				case UnicodeEscape:
				case OctalEscape:
				case InvalidEscape:
				case Alnum:
				case NotWhitespaceOrQuote:
					enterOuterAlt(_localctx, 2);
				{
					setState(36);
					nonQuoteArgument();
				}
				break;
				default:
					throw new NoViableAltException(this);
			}
		} catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		} finally {
			exitRule();
		}
		return _localctx;
	}

	public final NonQuoteArgumentContext nonQuoteArgument() throws RecognitionException {
		NonQuoteArgumentContext _localctx = new NonQuoteArgumentContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_nonQuoteArgument);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
				setState(40);
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
						{
							setState(39);
							_la = _input.LA(1);
							if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << RegularEscape) | (1L << UnicodeEscape) | (1L << OctalEscape) | (1L << InvalidEscape) | (1L << Alnum) | (1L << NotWhitespaceOrQuote))) != 0))) {
								_errHandler.recoverInline(this);
							} else {
								if (_input.LA(1) == Token.EOF) matchedEOF = true;
								_errHandler.reportMatch(this);
								consume();
							}
						}
					}
					setState(42);
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << RegularEscape) | (1L << UnicodeEscape) | (1L << OctalEscape) | (1L << InvalidEscape) | (1L << Alnum) | (1L << NotWhitespaceOrQuote))) != 0));
			}
		} catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		} finally {
			exitRule();
		}
		return _localctx;
	}

	public final QuoteArgumentContext quoteArgument() throws RecognitionException {
		QuoteArgumentContext _localctx = new QuoteArgumentContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_quoteArgument);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
				setState(44);
				match(Quote);
				setState(48);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << RegularEscape) | (1L << UnicodeEscape) | (1L << OctalEscape) | (1L << InvalidEscape) | (1L << Alnum) | (1L << Whitespace) | (1L << NotWhitespaceOrQuote))) != 0)) {
					{
						{
							setState(45);
							notQuote();
						}
					}
					setState(50);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(51);
				match(Quote);
			}
		} catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		} finally {
			exitRule();
		}
		return _localctx;
	}

	public final NotQuoteContext notQuote() throws RecognitionException {
		NotQuoteContext _localctx = new NotQuoteContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_notQuote);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
				setState(53);
				_la = _input.LA(1);
				if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << RegularEscape) | (1L << UnicodeEscape) | (1L << OctalEscape) | (1L << InvalidEscape) | (1L << Alnum) | (1L << Whitespace) | (1L << NotWhitespaceOrQuote))) != 0))) {
					_errHandler.recoverInline(this);
				} else {
					if (_input.LA(1) == Token.EOF) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
			}
		} catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		} finally {
			exitRule();
		}
		return _localctx;
	}

	public final CommandNameContext commandName() throws RecognitionException {
		CommandNameContext _localctx = new CommandNameContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_commandName);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
				setState(56);
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
						case 1: {
							{
								setState(55);
								match(Alnum);
							}
						}
						break;
						default:
							throw new NoViableAltException(this);
					}
					setState(58);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input, 7, _ctx);
				} while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER);
			}
		} catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		} finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CommandContext extends ParserRuleContext {
		public CommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}

		public CommandNameContext commandName() {
			return getRuleContext(CommandNameContext.class, 0);
		}

		public List<TerminalNode> EOF() {
			return getTokens(commandParser.EOF);
		}

		public TerminalNode EOF(int i) {
			return getToken(commandParser.EOF, i);
		}

		public List<TerminalNode> Whitespace() {
			return getTokens(commandParser.Whitespace);
		}

		public TerminalNode Whitespace(int i) {
			return getToken(commandParser.Whitespace, i);
		}

		public List<ArgumentContext> argument() {
			return getRuleContexts(ArgumentContext.class);
		}

		public ArgumentContext argument(int i) {
			return getRuleContext(ArgumentContext.class, i);
		}

		@Override
		public int getRuleIndex() {
			return RULE_command;
		}

		@Override
		public void enterRule(ParseTreeListener listener) {
			if (listener instanceof convergence.commandListener)
				((convergence.commandListener) listener).enterCommand(this);
		}

		@Override
		public void exitRule(ParseTreeListener listener) {
			if (listener instanceof convergence.commandListener)
				((convergence.commandListener) listener).exitCommand(this);
		}

		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if (visitor instanceof convergence.commandVisitor)
				return ((convergence.commandVisitor<? extends T>) visitor).visitCommand(this);
			else return visitor.visitChildren(this);
		}
	}

	public static class ArgumentContext extends ParserRuleContext {
		public ArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}

		public QuoteArgumentContext quoteArgument() {
			return getRuleContext(QuoteArgumentContext.class, 0);
		}

		public NonQuoteArgumentContext nonQuoteArgument() {
			return getRuleContext(NonQuoteArgumentContext.class, 0);
		}

		@Override
		public int getRuleIndex() {
			return RULE_argument;
		}

		@Override
		public void enterRule(ParseTreeListener listener) {
			if (listener instanceof convergence.commandListener)
				((convergence.commandListener) listener).enterArgument(this);
		}

		@Override
		public void exitRule(ParseTreeListener listener) {
			if (listener instanceof convergence.commandListener)
				((convergence.commandListener) listener).exitArgument(this);
		}

		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if (visitor instanceof convergence.commandVisitor)
				return ((convergence.commandVisitor<? extends T>) visitor).visitArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public static class NonQuoteArgumentContext extends ParserRuleContext {
		public NonQuoteArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}

		public List<TerminalNode> Alnum() {
			return getTokens(commandParser.Alnum);
		}

		public TerminalNode Alnum(int i) {
			return getToken(commandParser.Alnum, i);
		}

		public List<TerminalNode> RegularEscape() {
			return getTokens(commandParser.RegularEscape);
		}

		public TerminalNode RegularEscape(int i) {
			return getToken(commandParser.RegularEscape, i);
		}

		public List<TerminalNode> UnicodeEscape() {
			return getTokens(commandParser.UnicodeEscape);
		}

		public TerminalNode UnicodeEscape(int i) {
			return getToken(commandParser.UnicodeEscape, i);
		}

		public List<TerminalNode> OctalEscape() {
			return getTokens(commandParser.OctalEscape);
		}

		public TerminalNode OctalEscape(int i) {
			return getToken(commandParser.OctalEscape, i);
		}

		public List<TerminalNode> InvalidEscape() {
			return getTokens(commandParser.InvalidEscape);
		}

		public TerminalNode InvalidEscape(int i) {
			return getToken(commandParser.InvalidEscape, i);
		}

		public List<TerminalNode> NotWhitespaceOrQuote() {
			return getTokens(commandParser.NotWhitespaceOrQuote);
		}

		public TerminalNode NotWhitespaceOrQuote(int i) {
			return getToken(commandParser.NotWhitespaceOrQuote, i);
		}

		@Override
		public int getRuleIndex() {
			return RULE_nonQuoteArgument;
		}

		@Override
		public void enterRule(ParseTreeListener listener) {
			if (listener instanceof convergence.commandListener)
				((convergence.commandListener) listener).enterNonQuoteArgument(this);
		}

		@Override
		public void exitRule(ParseTreeListener listener) {
			if (listener instanceof convergence.commandListener)
				((convergence.commandListener) listener).exitNonQuoteArgument(this);
		}

		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if (visitor instanceof convergence.commandVisitor)
				return ((convergence.commandVisitor<? extends T>) visitor).visitNonQuoteArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public static class QuoteArgumentContext extends ParserRuleContext {
		public QuoteArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}

		public List<TerminalNode> Quote() {
			return getTokens(commandParser.Quote);
		}

		public TerminalNode Quote(int i) {
			return getToken(commandParser.Quote, i);
		}

		public List<NotQuoteContext> notQuote() {
			return getRuleContexts(NotQuoteContext.class);
		}

		public NotQuoteContext notQuote(int i) {
			return getRuleContext(NotQuoteContext.class, i);
		}

		@Override
		public int getRuleIndex() {
			return RULE_quoteArgument;
		}

		@Override
		public void enterRule(ParseTreeListener listener) {
			if (listener instanceof convergence.commandListener)
				((convergence.commandListener) listener).enterQuoteArgument(this);
		}

		@Override
		public void exitRule(ParseTreeListener listener) {
			if (listener instanceof convergence.commandListener)
				((convergence.commandListener) listener).exitQuoteArgument(this);
		}

		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if (visitor instanceof convergence.commandVisitor)
				return ((convergence.commandVisitor<? extends T>) visitor).visitQuoteArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public static class NotQuoteContext extends ParserRuleContext {
		public NotQuoteContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}

		public TerminalNode RegularEscape() {
			return getToken(commandParser.RegularEscape, 0);
		}

		public TerminalNode UnicodeEscape() {
			return getToken(commandParser.UnicodeEscape, 0);
		}

		public TerminalNode OctalEscape() {
			return getToken(commandParser.OctalEscape, 0);
		}

		public TerminalNode InvalidEscape() {
			return getToken(commandParser.InvalidEscape, 0);
		}

		public TerminalNode Alnum() {
			return getToken(commandParser.Alnum, 0);
		}

		public TerminalNode Whitespace() {
			return getToken(commandParser.Whitespace, 0);
		}

		public TerminalNode NotWhitespaceOrQuote() {
			return getToken(commandParser.NotWhitespaceOrQuote, 0);
		}

		@Override
		public int getRuleIndex() {
			return RULE_notQuote;
		}

		@Override
		public void enterRule(ParseTreeListener listener) {
			if (listener instanceof convergence.commandListener)
				((convergence.commandListener) listener).enterNotQuote(this);
		}

		@Override
		public void exitRule(ParseTreeListener listener) {
			if (listener instanceof convergence.commandListener)
				((convergence.commandListener) listener).exitNotQuote(this);
		}

		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if (visitor instanceof convergence.commandVisitor)
				return ((convergence.commandVisitor<? extends T>) visitor).visitNotQuote(this);
			else return visitor.visitChildren(this);
		}
	}

	public static class CommandNameContext extends ParserRuleContext {
		public CommandNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}

		public List<TerminalNode> Alnum() {
			return getTokens(commandParser.Alnum);
		}

		public TerminalNode Alnum(int i) {
			return getToken(commandParser.Alnum, i);
		}

		@Override
		public int getRuleIndex() {
			return RULE_commandName;
		}

		@Override
		public void enterRule(ParseTreeListener listener) {
			if (listener instanceof convergence.commandListener)
				((convergence.commandListener) listener).enterCommandName(this);
		}

		@Override
		public void exitRule(ParseTreeListener listener) {
			if (listener instanceof convergence.commandListener)
				((convergence.commandListener) listener).exitCommandName(this);
		}

		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if (visitor instanceof convergence.commandVisitor)
				return ((convergence.commandVisitor<? extends T>) visitor).visitCommandName(this);
			else return visitor.visitChildren(this);
		}
	}
}
