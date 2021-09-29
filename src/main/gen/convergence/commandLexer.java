// Generated from /home/nicholasdelello/IntelliJIDEAProjects/convergenceBot/src/main/kotlin/convergence/command.g4 by ANTLR 4.9.1

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class commandLexer extends Lexer {
	public static final int
			RegularEscape = 1, UnicodeEscape = 2, OctalEscape = 3, InvalidEscape = 4, Alnum = 5,
			Whitespace = 6, NotWhitespaceOrQuote = 7, Quote = 8;
	public static final String[] ruleNames = makeRuleNames();
	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	public static final String _serializedATN =
			"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\n\u00b0\b\1\4\2\t" +
					"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13" +
					"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22" +
					"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31" +
					"\4\32\t\32\4\33\t\33\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3" +
					"\4\5\4E\n\4\3\5\3\5\3\5\3\5\3\5\3\5\3\5\5\5N\n\5\3\6\3\6\3\6\3\6\3\6\3" +
					"\6\3\6\3\6\5\6X\n\6\3\7\3\7\3\7\3\7\5\7^\n\7\3\b\3\b\3\t\3\t\5\td\n\t" +
					"\3\t\5\tg\n\t\3\t\5\tj\n\t\3\t\3\t\3\t\3\t\3\t\5\tq\n\t\3\t\5\tt\n\t\5" +
					"\tv\n\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\5\n\u0084\n\n" +
					"\3\13\6\13\u0087\n\13\r\13\16\13\u0088\3\f\6\f\u008c\n\f\r\f\16\f\u008d" +
					"\3\r\6\r\u0091\n\r\r\r\16\r\u0092\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3" +
					"\21\3\22\3\22\3\23\3\23\3\24\3\24\3\25\3\25\3\26\3\26\3\27\3\27\3\30\3" +
					"\30\3\31\3\31\3\32\3\32\3\33\3\33\3\u0092\2\34\3\3\5\4\7\5\t\2\13\2\r" +
					"\6\17\2\21\2\23\2\25\7\27\b\31\t\33\n\35\2\37\2!\2#\2%\2\'\2)\2+\2-\2" +
					"/\2\61\2\63\2\65\2\3\2\17\n\2$$))^^ddhhppttvv\13\2$$))\62;^^ddhhppttv" +
					"w\5\2\62;C\\c|\f\2\13\17\"\"\u0087\u0087\u00a2\u00a2\u1682\u1682\u2002" +
					"\u200c\u202a\u202b\u2031\u2031\u2061\u2061\u3002\u3002\r\2\13\17\"\"$" +
					"$\u0087\u0087\u00a2\u00a2\u1682\u1682\u2002\u200c\u202a\u202b\u2031\u2031" +
					"\u2061\u2061\u3002\u3002\3\2\62\64\3\2\628\3\2\629\3\2\66;\3\2:;\3\2\63" +
					";\3\2\62;\5\2\62;CHch\2\u00af\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\r" +
					"\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\3\67\3\2" +
					"\2\2\5:\3\2\2\2\7A\3\2\2\2\tF\3\2\2\2\13W\3\2\2\2\rY\3\2\2\2\17_\3\2\2" +
					"\2\21u\3\2\2\2\23\u0083\3\2\2\2\25\u0086\3\2\2\2\27\u008b\3\2\2\2\31\u0090" +
					"\3\2\2\2\33\u0094\3\2\2\2\35\u0096\3\2\2\2\37\u0098\3\2\2\2!\u009a\3\2" +
					"\2\2#\u009c\3\2\2\2%\u009e\3\2\2\2\'\u00a0\3\2\2\2)\u00a2\3\2\2\2+\u00a4" +
					"\3\2\2\2-\u00a6\3\2\2\2/\u00a8\3\2\2\2\61\u00aa\3\2\2\2\63\u00ac\3\2\2" +
					"\2\65\u00ae\3\2\2\2\678\5/\30\289\t\2\2\29\4\3\2\2\2:;\5/\30\2;<\5\61" +
					"\31\2<=\5\63\32\2=>\5\63\32\2>?\5\63\32\2?@\5\63\32\2@\6\3\2\2\2AD\5/" +
					"\30\2BE\5\t\5\2CE\5\13\6\2DB\3\2\2\2DC\3\2\2\2E\b\3\2\2\2FM\5\35\17\2" +
					"GH\5\37\20\2HI\5%\23\2IN\3\2\2\2JK\5#\22\2KL\5-\27\2LN\3\2\2\2MG\3\2\2" +
					"\2MJ\3\2\2\2N\n\3\2\2\2OP\5!\21\2PQ\5-\27\2QR\5-\27\2RX\3\2\2\2ST\5+\26" +
					"\2TU\5-\27\2UX\3\2\2\2VX\5-\27\2WO\3\2\2\2WS\3\2\2\2WV\3\2\2\2X\f\3\2" +
					"\2\2Y]\5/\30\2Z^\5\17\b\2[^\5\21\t\2\\^\5\23\n\2]Z\3\2\2\2][\3\2\2\2]" +
					"\\\3\2\2\2]^\3\2\2\2^\16\3\2\2\2_`\n\3\2\2`\20\3\2\2\2ac\5\61\31\2bd\5" +
					"\63\32\2cb\3\2\2\2cd\3\2\2\2df\3\2\2\2eg\5\63\32\2fe\3\2\2\2fg\3\2\2\2" +
					"gi\3\2\2\2hj\5\63\32\2ih\3\2\2\2ij\3\2\2\2jk\3\2\2\2kl\5\65\33\2lv\3\2" +
					"\2\2mn\5\61\31\2np\5\63\32\2oq\5\63\32\2po\3\2\2\2pq\3\2\2\2qs\3\2\2\2" +
					"rt\5\63\32\2sr\3\2\2\2st\3\2\2\2tv\3\2\2\2ua\3\2\2\2um\3\2\2\2v\22\3\2" +
					"\2\2wx\5\'\24\2xy\5-\27\2yz\5-\27\2z\u0084\3\2\2\2{|\5\35\17\2|}\5)\25" +
					"\2}~\5-\27\2~\u0084\3\2\2\2\177\u0080\5\35\17\2\u0080\u0081\5\37\20\2" +
					"\u0081\u0082\5)\25\2\u0082\u0084\3\2\2\2\u0083w\3\2\2\2\u0083{\3\2\2\2" +
					"\u0083\177\3\2\2\2\u0084\24\3\2\2\2\u0085\u0087\t\4\2\2\u0086\u0085\3" +
					"\2\2\2\u0087\u0088\3\2\2\2\u0088\u0086\3\2\2\2\u0088\u0089\3\2\2\2\u0089" +
					"\26\3\2\2\2\u008a\u008c\t\5\2\2\u008b\u008a\3\2\2\2\u008c\u008d\3\2\2" +
					"\2\u008d\u008b\3\2\2\2\u008d\u008e\3\2\2\2\u008e\30\3\2\2\2\u008f\u0091" +
					"\n\6\2\2\u0090\u008f\3\2\2\2\u0091\u0092\3\2\2\2\u0092\u0093\3\2\2\2\u0092" +
					"\u0090\3\2\2\2\u0093\32\3\2\2\2\u0094\u0095\7$\2\2\u0095\34\3\2\2\2\u0096" +
					"\u0097\7\65\2\2\u0097\36\3\2\2\2\u0098\u0099\79\2\2\u0099 \3\2\2\2\u009a" +
					"\u009b\t\7\2\2\u009b\"\3\2\2\2\u009c\u009d\t\b\2\2\u009d$\3\2\2\2\u009e" +
					"\u009f\t\t\2\2\u009f&\3\2\2\2\u00a0\u00a1\t\n\2\2\u00a1(\3\2\2\2\u00a2" +
					"\u00a3\t\13\2\2\u00a3*\3\2\2\2\u00a4\u00a5\t\f\2\2\u00a5,\3\2\2\2\u00a6" +
					"\u00a7\t\r\2\2\u00a7.\3\2\2\2\u00a8\u00a9\7^\2\2\u00a9\60\3\2\2\2\u00aa" +
					"\u00ab\7w\2\2\u00ab\62\3\2\2\2\u00ac\u00ad\t\16\2\2\u00ad\64\3\2\2\2\u00ae" +
					"\u00af\n\16\2\2\u00af\66\3\2\2\2\21\2DMW]cfipsu\u0083\u0088\u008d\u0092" +
					"\2";
	public static final ATN _ATN =
			new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
			new PredictionContextCache();
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);
	public static String[] channelNames = {
			"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};
	public static String[] modeNames = {
			"DEFAULT_MODE"
	};

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

	public commandLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
	}

	private static String[] makeRuleNames() {
		return new String[]{
				"RegularEscape", "UnicodeEscape", "OctalEscape", "ThreeHundredAndOver",
				"TwoHundredOrLess", "InvalidEscape", "InvalidRegularEscape", "InvalidUnicodeEscape",
				"InvalidOctalEscape", "Alnum", "Whitespace", "NotWhitespaceOrQuote",
				"Quote", "THREE", "SEVEN", "ZeroToTwo", "ZeroToSix", "ZeroToSeven", "FourToNine",
				"EightOrNine", "OneToNine", "Number", "Backslash", "U", "UnicodeDigit",
				"NotUnicodeDigit"
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
	public String[] getChannelNames() {
		return channelNames;
	}

	@Override
	public String[] getModeNames() {
		return modeNames;
	}

	@Override
	public ATN getATN() {
		return _ATN;
	}
}
