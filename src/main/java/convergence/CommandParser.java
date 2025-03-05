// Generated from /home/nicholasdelello/IntelliJIDEAProjects/convergenceBot/src/main/kotlin/convergence/Command.g4 by ANTLR 4.13.2
package convergence;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class CommandParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		RegularEscape=1, UnicodeEscape=2, OctalEscape=3, InvalidEscape=4, Alnum=5, 
		Whitespace=6, NotWhitespaceOrQuote=7, Quote=8;
	public static final int
		RULE_command = 0, RULE_argument = 1, RULE_nonQuoteArgument = 2, RULE_quoteArgument = 3, 
		RULE_notQuote = 4, RULE_commandName = 5;
	private static String[] makeRuleNames() {
		return new String[] {
			"command", "argument", "nonQuoteArgument", "quoteArgument", "notQuote", 
			"commandName"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, "'\"'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "RegularEscape", "UnicodeEscape", "OctalEscape", "InvalidEscape", 
			"Alnum", "Whitespace", "NotWhitespaceOrQuote", "Quote"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
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
	public String getGrammarFileName() { return "Command.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public CommandParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CommandContext extends ParserRuleContext {
		public CommandNameContext commandName() {
			return getRuleContext(CommandNameContext.class,0);
		}
		public List<TerminalNode> EOF() { return getTokens(CommandParser.EOF); }
		public TerminalNode EOF(int i) {
			return getToken(CommandParser.EOF, i);
		}
		public List<TerminalNode> Whitespace() { return getTokens(CommandParser.Whitespace); }
		public TerminalNode Whitespace(int i) {
			return getToken(CommandParser.Whitespace, i);
		}
		public List<ArgumentContext> argument() {
			return getRuleContexts(ArgumentContext.class);
		}
		public ArgumentContext argument(int i) {
			return getRuleContext(ArgumentContext.class,i);
		}
		public CommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_command; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CommandListener ) ((CommandListener)listener).enterCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CommandListener ) ((CommandListener)listener).exitCommand(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CommandVisitor ) return ((CommandVisitor<? extends T>)visitor).visitCommand(this);
			else return visitor.visitChildren(this);
		}
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
			while (_la==Whitespace) {
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
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 446L) != 0)) {
				{
				{
				setState(19);
				argument();
				setState(26);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case Whitespace:
					{
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
					} while ( _la==Whitespace );
					}
					break;
				case EOF:
					{
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
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArgumentContext extends ParserRuleContext {
		public QuoteArgumentContext quoteArgument() {
			return getRuleContext(QuoteArgumentContext.class,0);
		}
		public NonQuoteArgumentContext nonQuoteArgument() {
			return getRuleContext(NonQuoteArgumentContext.class,0);
		}
		public ArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CommandListener ) ((CommandListener)listener).enterArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CommandListener ) ((CommandListener)listener).exitArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CommandVisitor ) return ((CommandVisitor<? extends T>)visitor).visitArgument(this);
			else return visitor.visitChildren(this);
		}
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
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NonQuoteArgumentContext extends ParserRuleContext {
		public List<TerminalNode> RegularEscape() { return getTokens(CommandParser.RegularEscape); }
		public TerminalNode RegularEscape(int i) {
			return getToken(CommandParser.RegularEscape, i);
		}
		public List<TerminalNode> UnicodeEscape() { return getTokens(CommandParser.UnicodeEscape); }
		public TerminalNode UnicodeEscape(int i) {
			return getToken(CommandParser.UnicodeEscape, i);
		}
		public List<TerminalNode> OctalEscape() { return getTokens(CommandParser.OctalEscape); }
		public TerminalNode OctalEscape(int i) {
			return getToken(CommandParser.OctalEscape, i);
		}
		public List<TerminalNode> InvalidEscape() { return getTokens(CommandParser.InvalidEscape); }
		public TerminalNode InvalidEscape(int i) {
			return getToken(CommandParser.InvalidEscape, i);
		}
		public List<TerminalNode> Alnum() { return getTokens(CommandParser.Alnum); }
		public TerminalNode Alnum(int i) {
			return getToken(CommandParser.Alnum, i);
		}
		public List<TerminalNode> NotWhitespaceOrQuote() { return getTokens(CommandParser.NotWhitespaceOrQuote); }
		public TerminalNode NotWhitespaceOrQuote(int i) {
			return getToken(CommandParser.NotWhitespaceOrQuote, i);
		}
		public NonQuoteArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nonQuoteArgument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CommandListener ) ((CommandListener)listener).enterNonQuoteArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CommandListener ) ((CommandListener)listener).exitNonQuoteArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CommandVisitor ) return ((CommandVisitor<? extends T>)visitor).visitNonQuoteArgument(this);
			else return visitor.visitChildren(this);
		}
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
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 190L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				}
				setState(42); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 190L) != 0) );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class QuoteArgumentContext extends ParserRuleContext {
		public List<TerminalNode> Quote() { return getTokens(CommandParser.Quote); }
		public TerminalNode Quote(int i) {
			return getToken(CommandParser.Quote, i);
		}
		public List<NotQuoteContext> notQuote() {
			return getRuleContexts(NotQuoteContext.class);
		}
		public NotQuoteContext notQuote(int i) {
			return getRuleContext(NotQuoteContext.class,i);
		}
		public QuoteArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_quoteArgument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CommandListener ) ((CommandListener)listener).enterQuoteArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CommandListener ) ((CommandListener)listener).exitQuoteArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CommandVisitor ) return ((CommandVisitor<? extends T>)visitor).visitQuoteArgument(this);
			else return visitor.visitChildren(this);
		}
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
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 254L) != 0)) {
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
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NotQuoteContext extends ParserRuleContext {
		public TerminalNode RegularEscape() { return getToken(CommandParser.RegularEscape, 0); }
		public TerminalNode UnicodeEscape() { return getToken(CommandParser.UnicodeEscape, 0); }
		public TerminalNode OctalEscape() { return getToken(CommandParser.OctalEscape, 0); }
		public TerminalNode InvalidEscape() { return getToken(CommandParser.InvalidEscape, 0); }
		public TerminalNode Alnum() { return getToken(CommandParser.Alnum, 0); }
		public TerminalNode Whitespace() { return getToken(CommandParser.Whitespace, 0); }
		public TerminalNode NotWhitespaceOrQuote() { return getToken(CommandParser.NotWhitespaceOrQuote, 0); }
		public NotQuoteContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_notQuote; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CommandListener ) ((CommandListener)listener).enterNotQuote(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CommandListener ) ((CommandListener)listener).exitNotQuote(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CommandVisitor ) return ((CommandVisitor<? extends T>)visitor).visitNotQuote(this);
			else return visitor.visitChildren(this);
		}
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
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 254L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CommandNameContext extends ParserRuleContext {
		public List<TerminalNode> Alnum() { return getTokens(CommandParser.Alnum); }
		public TerminalNode Alnum(int i) {
			return getToken(CommandParser.Alnum, i);
		}
		public CommandNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_commandName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof CommandListener ) ((CommandListener)listener).enterCommandName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof CommandListener ) ((CommandListener)listener).exitCommandName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof CommandVisitor ) return ((CommandVisitor<? extends T>)visitor).visitCommandName(this);
			else return visitor.visitChildren(this);
		}
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
				case 1:
					{
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
				_alt = getInterpreter().adaptivePredict(_input,7,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\u0004\u0001\b=\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0001\u0000\u0001\u0000\u0005\u0000\u000f\b\u0000\n"+
		"\u0000\f\u0000\u0012\t\u0000\u0001\u0000\u0001\u0000\u0004\u0000\u0016"+
		"\b\u0000\u000b\u0000\f\u0000\u0017\u0001\u0000\u0003\u0000\u001b\b\u0000"+
		"\u0005\u0000\u001d\b\u0000\n\u0000\f\u0000 \t\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0001\u0001\u0001\u0003\u0001&\b\u0001\u0001\u0002\u0004"+
		"\u0002)\b\u0002\u000b\u0002\f\u0002*\u0001\u0003\u0001\u0003\u0005\u0003"+
		"/\b\u0003\n\u0003\f\u00032\t\u0003\u0001\u0003\u0001\u0003\u0001\u0004"+
		"\u0001\u0004\u0001\u0005\u0004\u00059\b\u0005\u000b\u0005\f\u0005:\u0001"+
		"\u0005\u0000\u0000\u0006\u0000\u0002\u0004\u0006\b\n\u0000\u0002\u0002"+
		"\u0000\u0001\u0005\u0007\u0007\u0001\u0000\u0001\u0007>\u0000\f\u0001"+
		"\u0000\u0000\u0000\u0002%\u0001\u0000\u0000\u0000\u0004(\u0001\u0000\u0000"+
		"\u0000\u0006,\u0001\u0000\u0000\u0000\b5\u0001\u0000\u0000\u0000\n8\u0001"+
		"\u0000\u0000\u0000\f\u0010\u0003\n\u0005\u0000\r\u000f\u0005\u0006\u0000"+
		"\u0000\u000e\r\u0001\u0000\u0000\u0000\u000f\u0012\u0001\u0000\u0000\u0000"+
		"\u0010\u000e\u0001\u0000\u0000\u0000\u0010\u0011\u0001\u0000\u0000\u0000"+
		"\u0011\u001e\u0001\u0000\u0000\u0000\u0012\u0010\u0001\u0000\u0000\u0000"+
		"\u0013\u001a\u0003\u0002\u0001\u0000\u0014\u0016\u0005\u0006\u0000\u0000"+
		"\u0015\u0014\u0001\u0000\u0000\u0000\u0016\u0017\u0001\u0000\u0000\u0000"+
		"\u0017\u0015\u0001\u0000\u0000\u0000\u0017\u0018\u0001\u0000\u0000\u0000"+
		"\u0018\u001b\u0001\u0000\u0000\u0000\u0019\u001b\u0005\u0000\u0000\u0001"+
		"\u001a\u0015\u0001\u0000\u0000\u0000\u001a\u0019\u0001\u0000\u0000\u0000"+
		"\u001b\u001d\u0001\u0000\u0000\u0000\u001c\u0013\u0001\u0000\u0000\u0000"+
		"\u001d \u0001\u0000\u0000\u0000\u001e\u001c\u0001\u0000\u0000\u0000\u001e"+
		"\u001f\u0001\u0000\u0000\u0000\u001f!\u0001\u0000\u0000\u0000 \u001e\u0001"+
		"\u0000\u0000\u0000!\"\u0005\u0000\u0000\u0001\"\u0001\u0001\u0000\u0000"+
		"\u0000#&\u0003\u0006\u0003\u0000$&\u0003\u0004\u0002\u0000%#\u0001\u0000"+
		"\u0000\u0000%$\u0001\u0000\u0000\u0000&\u0003\u0001\u0000\u0000\u0000"+
		"\')\u0007\u0000\u0000\u0000(\'\u0001\u0000\u0000\u0000)*\u0001\u0000\u0000"+
		"\u0000*(\u0001\u0000\u0000\u0000*+\u0001\u0000\u0000\u0000+\u0005\u0001"+
		"\u0000\u0000\u0000,0\u0005\b\u0000\u0000-/\u0003\b\u0004\u0000.-\u0001"+
		"\u0000\u0000\u0000/2\u0001\u0000\u0000\u00000.\u0001\u0000\u0000\u0000"+
		"01\u0001\u0000\u0000\u000013\u0001\u0000\u0000\u000020\u0001\u0000\u0000"+
		"\u000034\u0005\b\u0000\u00004\u0007\u0001\u0000\u0000\u000056\u0007\u0001"+
		"\u0000\u00006\t\u0001\u0000\u0000\u000079\u0005\u0005\u0000\u000087\u0001"+
		"\u0000\u0000\u00009:\u0001\u0000\u0000\u0000:8\u0001\u0000\u0000\u0000"+
		":;\u0001\u0000\u0000\u0000;\u000b\u0001\u0000\u0000\u0000\b\u0010\u0017"+
		"\u001a\u001e%*0:";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
