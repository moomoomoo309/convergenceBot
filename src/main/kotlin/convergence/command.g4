grammar command;

// Parser Rules

command: commandName Whitespace* (argument (Whitespace+ | EOF))* EOF;
argument: quoteArgument | nonQuoteArgument;
nonQuoteArgument: (Alnum | RegularEscape | UnicodeEscape | OctalEscape | InvalidEscape | NotWhitespaceOrQuote)+;
quoteArgument: Quote notQuote* Quote;
notQuote: RegularEscape | UnicodeEscape | OctalEscape | InvalidEscape | Alnum | Whitespace | NotWhitespaceOrQuote;
commandName: Alnum+;

// Lexer Rules
RegularEscape: Backslash [tbnrf'"\\];

UnicodeEscape: Backslash U UnicodeDigit UnicodeDigit UnicodeDigit UnicodeDigit;

OctalEscape: Backslash (ThreeHundredAndOver | TwoHundredOrLess);
fragment ThreeHundredAndOver: THREE (SEVEN ZeroToSeven | ZeroToSix Number);
fragment TwoHundredOrLess: ZeroToTwo Number Number | OneToNine Number | Number;

InvalidEscape: Backslash (InvalidRegularEscape | InvalidUnicodeEscape | InvalidOctalEscape)?;
fragment InvalidRegularEscape: ~[tbnrf'"\\u0-9];
fragment InvalidUnicodeEscape: U UnicodeDigit? UnicodeDigit? UnicodeDigit? NotUnicodeDigit | U UnicodeDigit UnicodeDigit? UnicodeDigit?;
fragment InvalidOctalEscape: FourToNine Number Number | THREE EightOrNine Number | THREE SEVEN EightOrNine;

Alnum: [a-zA-Z0-9]+;
Whitespace: [\p{White_Space}]+;
NotWhitespaceOrQuote: ~[\p{White_Space}"]+?;
Quote: '"';

fragment THREE: '3';
fragment SEVEN: '7';
fragment ZeroToTwo: [0-2];
fragment ZeroToSix: [0-6];
fragment ZeroToSeven: [0-7];
fragment FourToNine: [4-9];
fragment EightOrNine: [89];
fragment OneToNine: [1-9];
fragment Number: [0-9];
fragment Backslash: '\\';
fragment U: 'u';
fragment UnicodeDigit: [0-9a-fA-F];
fragment NotUnicodeDigit: ~[0-9a-fA-F];
