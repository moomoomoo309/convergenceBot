// Generated from /home/nicholasdelello/IntelliJIDEAProjects/convergenceBot/src/main/kotlin/convergence/command.g4 by ANTLR 4.13.1
package convergence;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
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
            "\u0004\u0000\b\u00ae\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001" +
                    "\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004" +
                    "\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007" +
                    "\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b" +
                    "\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002" +
                    "\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002" +
                    "\u0012\u0007\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002" +
                    "\u0015\u0007\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002" +
                    "\u0018\u0007\u0018\u0002\u0019\u0007\u0019\u0001\u0000\u0001\u0000\u0001" +
                    "\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
                    "\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002C\b" +
                    "\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001" +
                    "\u0003\u0001\u0003\u0003\u0003L\b\u0003\u0001\u0004\u0001\u0004\u0001" +
                    "\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003" +
                    "\u0004V\b\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0003" +
                    "\u0005\\\b\u0005\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0003" +
                    "\u0007b\b\u0007\u0001\u0007\u0003\u0007e\b\u0007\u0001\u0007\u0003\u0007" +
                    "h\b\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007" +
                    "\u0003\u0007o\b\u0007\u0001\u0007\u0003\u0007r\b\u0007\u0003\u0007t\b" +
                    "\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b" +
                    "\u0001\b\u0001\b\u0001\b\u0001\b\u0003\b\u0082\b\b\u0001\t\u0004\t\u0085" +
                    "\b\t\u000b\t\f\t\u0086\u0001\n\u0004\n\u008a\b\n\u000b\n\f\n\u008b\u0001" +
                    "\u000b\u0004\u000b\u008f\b\u000b\u000b\u000b\f\u000b\u0090\u0001\f\u0001" +
                    "\f\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000f\u0001\u000f\u0001" +
                    "\u0010\u0001\u0010\u0001\u0011\u0001\u0011\u0001\u0012\u0001\u0012\u0001" +
                    "\u0013\u0001\u0013\u0001\u0014\u0001\u0014\u0001\u0015\u0001\u0015\u0001" +
                    "\u0016\u0001\u0016\u0001\u0017\u0001\u0017\u0001\u0018\u0001\u0018\u0001" +
                    "\u0019\u0001\u0019\u0001\u0090\u0000\u001a\u0001\u0001\u0003\u0002\u0005" +
                    "\u0003\u0007\u0000\t\u0000\u000b\u0004\r\u0000\u000f\u0000\u0011\u0000" +
                    "\u0013\u0005\u0015\u0006\u0017\u0007\u0019\b\u001b\u0000\u001d\u0000\u001f" +
                    "\u0000!\u0000#\u0000%\u0000\'\u0000)\u0000+\u0000-\u0000/\u00001\u0000" +
                    "3\u0000\u0001\u0000\r\b\u0000\"\"\'\'\\\\bbffnnrrtt\t\u0000\"\"\'\'09" +
                    "\\\\bbffnnrrtu\u0003\u000009AZaz\n\u0000\t\r  \u0085\u0085\u00a0\u00a0" +
                    "\u1680\u1680\u2000\u200a\u2028\u2029\u202f\u202f\u205f\u205f\u3000\u3000" +
                    "\u000b\u0000\t\r  \"\"\u0085\u0085\u00a0\u00a0\u1680\u1680\u2000\u200a" +
                    "\u2028\u2029\u202f\u202f\u205f\u205f\u3000\u3000\u0001\u000002\u0001\u0000" +
                    "06\u0001\u000007\u0001\u000049\u0001\u000089\u0001\u000019\u0001\u0000" +
                    "09\u0003\u000009AFaf\u00ad\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0003" +
                    "\u0001\u0000\u0000\u0000\u0000\u0005\u0001\u0000\u0000\u0000\u0000\u000b" +
                    "\u0001\u0000\u0000\u0000\u0000\u0013\u0001\u0000\u0000\u0000\u0000\u0015" +
                    "\u0001\u0000\u0000\u0000\u0000\u0017\u0001\u0000\u0000\u0000\u0000\u0019" +
                    "\u0001\u0000\u0000\u0000\u00015\u0001\u0000\u0000\u0000\u00038\u0001\u0000" +
                    "\u0000\u0000\u0005?\u0001\u0000\u0000\u0000\u0007D\u0001\u0000\u0000\u0000" +
                    "\tU\u0001\u0000\u0000\u0000\u000bW\u0001\u0000\u0000\u0000\r]\u0001\u0000" +
                    "\u0000\u0000\u000fs\u0001\u0000\u0000\u0000\u0011\u0081\u0001\u0000\u0000" +
                    "\u0000\u0013\u0084\u0001\u0000\u0000\u0000\u0015\u0089\u0001\u0000\u0000" +
                    "\u0000\u0017\u008e\u0001\u0000\u0000\u0000\u0019\u0092\u0001\u0000\u0000" +
                    "\u0000\u001b\u0094\u0001\u0000\u0000\u0000\u001d\u0096\u0001\u0000\u0000" +
                    "\u0000\u001f\u0098\u0001\u0000\u0000\u0000!\u009a\u0001\u0000\u0000\u0000" +
                    "#\u009c\u0001\u0000\u0000\u0000%\u009e\u0001\u0000\u0000\u0000\'\u00a0" +
                    "\u0001\u0000\u0000\u0000)\u00a2\u0001\u0000\u0000\u0000+\u00a4\u0001\u0000" +
                    "\u0000\u0000-\u00a6\u0001\u0000\u0000\u0000/\u00a8\u0001\u0000\u0000\u0000" +
                    "1\u00aa\u0001\u0000\u0000\u00003\u00ac\u0001\u0000\u0000\u000056\u0003" +
                    "-\u0016\u000067\u0007\u0000\u0000\u00007\u0002\u0001\u0000\u0000\u0000" +
                    "89\u0003-\u0016\u00009:\u0003/\u0017\u0000:;\u00031\u0018\u0000;<\u0003" +
                    "1\u0018\u0000<=\u00031\u0018\u0000=>\u00031\u0018\u0000>\u0004\u0001\u0000" +
                    "\u0000\u0000?B\u0003-\u0016\u0000@C\u0003\u0007\u0003\u0000AC\u0003\t" +
                    "\u0004\u0000B@\u0001\u0000\u0000\u0000BA\u0001\u0000\u0000\u0000C\u0006" +
                    "\u0001\u0000\u0000\u0000DK\u0003\u001b\r\u0000EF\u0003\u001d\u000e\u0000" +
                    "FG\u0003#\u0011\u0000GL\u0001\u0000\u0000\u0000HI\u0003!\u0010\u0000I" +
                    "J\u0003+\u0015\u0000JL\u0001\u0000\u0000\u0000KE\u0001\u0000\u0000\u0000" +
                    "KH\u0001\u0000\u0000\u0000L\b\u0001\u0000\u0000\u0000MN\u0003\u001f\u000f" +
                    "\u0000NO\u0003+\u0015\u0000OP\u0003+\u0015\u0000PV\u0001\u0000\u0000\u0000" +
                    "QR\u0003)\u0014\u0000RS\u0003+\u0015\u0000SV\u0001\u0000\u0000\u0000T" +
                    "V\u0003+\u0015\u0000UM\u0001\u0000\u0000\u0000UQ\u0001\u0000\u0000\u0000" +
                    "UT\u0001\u0000\u0000\u0000V\n\u0001\u0000\u0000\u0000W[\u0003-\u0016\u0000" +
                    "X\\\u0003\r\u0006\u0000Y\\\u0003\u000f\u0007\u0000Z\\\u0003\u0011\b\u0000" +
                    "[X\u0001\u0000\u0000\u0000[Y\u0001\u0000\u0000\u0000[Z\u0001\u0000\u0000" +
                    "\u0000[\\\u0001\u0000\u0000\u0000\\\f\u0001\u0000\u0000\u0000]^\b\u0001" +
                    "\u0000\u0000^\u000e\u0001\u0000\u0000\u0000_a\u0003/\u0017\u0000`b\u0003" +
                    "1\u0018\u0000a`\u0001\u0000\u0000\u0000ab\u0001\u0000\u0000\u0000bd\u0001" +
                    "\u0000\u0000\u0000ce\u00031\u0018\u0000dc\u0001\u0000\u0000\u0000de\u0001" +
                    "\u0000\u0000\u0000eg\u0001\u0000\u0000\u0000fh\u00031\u0018\u0000gf\u0001" +
                    "\u0000\u0000\u0000gh\u0001\u0000\u0000\u0000hi\u0001\u0000\u0000\u0000" +
                    "ij\u00033\u0019\u0000jt\u0001\u0000\u0000\u0000kl\u0003/\u0017\u0000l" +
                    "n\u00031\u0018\u0000mo\u00031\u0018\u0000nm\u0001\u0000\u0000\u0000no" +
                    "\u0001\u0000\u0000\u0000oq\u0001\u0000\u0000\u0000pr\u00031\u0018\u0000" +
                    "qp\u0001\u0000\u0000\u0000qr\u0001\u0000\u0000\u0000rt\u0001\u0000\u0000" +
                    "\u0000s_\u0001\u0000\u0000\u0000sk\u0001\u0000\u0000\u0000t\u0010\u0001" +
                    "\u0000\u0000\u0000uv\u0003%\u0012\u0000vw\u0003+\u0015\u0000wx\u0003+" +
                    "\u0015\u0000x\u0082\u0001\u0000\u0000\u0000yz\u0003\u001b\r\u0000z{\u0003" +
                    "\'\u0013\u0000{|\u0003+\u0015\u0000|\u0082\u0001\u0000\u0000\u0000}~\u0003" +
                    "\u001b\r\u0000~\u007f\u0003\u001d\u000e\u0000\u007f\u0080\u0003\'\u0013" +
                    "\u0000\u0080\u0082\u0001\u0000\u0000\u0000\u0081u\u0001\u0000\u0000\u0000" +
                    "\u0081y\u0001\u0000\u0000\u0000\u0081}\u0001\u0000\u0000\u0000\u0082\u0012" +
                    "\u0001\u0000\u0000\u0000\u0083\u0085\u0007\u0002\u0000\u0000\u0084\u0083" +
                    "\u0001\u0000\u0000\u0000\u0085\u0086\u0001\u0000\u0000\u0000\u0086\u0084" +
                    "\u0001\u0000\u0000\u0000\u0086\u0087\u0001\u0000\u0000\u0000\u0087\u0014" +
                    "\u0001\u0000\u0000\u0000\u0088\u008a\u0007\u0003\u0000\u0000\u0089\u0088" +
                    "\u0001\u0000\u0000\u0000\u008a\u008b\u0001\u0000\u0000\u0000\u008b\u0089" +
                    "\u0001\u0000\u0000\u0000\u008b\u008c\u0001\u0000\u0000\u0000\u008c\u0016" +
                    "\u0001\u0000\u0000\u0000\u008d\u008f\b\u0004\u0000\u0000\u008e\u008d\u0001" +
                    "\u0000\u0000\u0000\u008f\u0090\u0001\u0000\u0000\u0000\u0090\u0091\u0001" +
                    "\u0000\u0000\u0000\u0090\u008e\u0001\u0000\u0000\u0000\u0091\u0018\u0001" +
                    "\u0000\u0000\u0000\u0092\u0093\u0005\"\u0000\u0000\u0093\u001a\u0001\u0000" +
                    "\u0000\u0000\u0094\u0095\u00053\u0000\u0000\u0095\u001c\u0001\u0000\u0000" +
                    "\u0000\u0096\u0097\u00057\u0000\u0000\u0097\u001e\u0001\u0000\u0000\u0000" +
                    "\u0098\u0099\u0007\u0005\u0000\u0000\u0099 \u0001\u0000\u0000\u0000\u009a" +
                    "\u009b\u0007\u0006\u0000\u0000\u009b\"\u0001\u0000\u0000\u0000\u009c\u009d" +
                    "\u0007\u0007\u0000\u0000\u009d$\u0001\u0000\u0000\u0000\u009e\u009f\u0007" +
                    "\b\u0000\u0000\u009f&\u0001\u0000\u0000\u0000\u00a0\u00a1\u0007\t\u0000" +
                    "\u0000\u00a1(\u0001\u0000\u0000\u0000\u00a2\u00a3\u0007\n\u0000\u0000" +
                    "\u00a3*\u0001\u0000\u0000\u0000\u00a4\u00a5\u0007\u000b\u0000\u0000\u00a5" +
                    ",\u0001\u0000\u0000\u0000\u00a6\u00a7\u0005\\\u0000\u0000\u00a7.\u0001" +
                    "\u0000\u0000\u0000\u00a8\u00a9\u0005u\u0000\u0000\u00a90\u0001\u0000\u0000" +
                    "\u0000\u00aa\u00ab\u0007\f\u0000\u0000\u00ab2\u0001\u0000\u0000\u0000" +
                    "\u00ac\u00ad\b\f\u0000\u0000\u00ad4\u0001\u0000\u0000\u0000\u000f\u0000" +
                    "BKU[adgnqs\u0081\u0086\u008b\u0090\u0000";
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
        RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION);
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

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
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

	public Vocabulary getVocabulary() {
		return VOCABULARY;
    }

	@Override
    public String getGrammarFileName() {
        return "command.g4"; }

	@Override
    public String[] getRuleNames() {
        return ruleNames; }

	@Override
    public String getSerializedATN() {
        return _serializedATN; }

	@Override
    public String[] getChannelNames() {
        return channelNames; }

	@Override
    public String[] getModeNames() {
        return modeNames; }

	@Override
    public ATN getATN() {
        return _ATN;
    }
}
