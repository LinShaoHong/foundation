grammar ExpressionGrammar;

@lexer::header {
package com.github.sun.foundation.expression.parser;
}

@parser::header {
package com.github.sun.foundation.expression.parser;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.expression.ExpressionBuilder;
}

@parser::members {
protected ExpressionBuilder builder;
}

start returns[Expression value]:
    a = expr EOF    { $value = $a.value; }
    ;

expr returns[Expression value]:
    a = or_expr     { $value = $a.value; }
    ;

or_expr returns[Expression value]:
    a = and_expr              { $value = $a.value; }
    ( '||' b = and_expr       { $value = builder.or($value, $b.value); } )*
    ;

and_expr returns[Expression value]:
    a = not_expr              { $value = $a.value; }
    ( '&&' b = not_expr       { $value = builder.and($value, $b.value); } )*
    ;

not_expr returns[Expression value]:
    t = '!'? a = comp_expr    { $value = $t == null? $a.value : builder.not($a.value); }
    ;

comp_expr returns[Expression value]:
    a = plus_expr             { $value = $a.value; }
    ( '==' b = plus_expr      { $value = builder.eq($value, $b.value); }
      | '>=' b = plus_expr    { $value = builder.ge($value, $b.value); }
      | '>' b = plus_expr     { $value = builder.gt($value, $b.value); }
      | '<=' b = plus_expr    { $value = builder.le($value, $b.value); }
      | '<' b = plus_expr     { $value = builder.lt($value, $b.value); }
      | '!=' b = plus_expr    { $value = builder.ne($value, $b.value); }
    )*
    ;

plus_expr returns[Expression value]:
    a = mul_expr              { $value = $a.value; }
    ( '+' b = mul_expr        { $value = builder.plus($value, $b.value); }
      | '-' b = mul_expr      { $value = builder.sub($value, $b.value); }
    )*
    ;

mul_expr returns[Expression value]:
    a = call_expr             { $value = $a.value; }
    ( '*' b = call_expr       { $value = builder.mul($value, $b.value); }
      | '/' b = call_expr     { $value = builder.div($value, $b.value); }
      | '%' b = call_expr     { $value = builder.mod($value, $b.value); }
    )*
    ;

call_expr returns[Expression value]:
    a = member_expr           { $value = $a.value; }
    ( '()'                    { $value = builder.call($value); }
      |'(' b = params ')'     { $value = builder.call($value, $b.value); }
     )?
    ;

member_expr returns[Expression value]:
    a = atom_expr             { $value = $a.value; }
    ( '.' t = ID              { $value = builder.member($value, $t.text); }
      | '.' t = QuotedID      { $value = builder.member($value, builder.unquote($t.text)); }
    )*
    ;

atom_expr returns[Expression value]:
    '(' a = expr ')'          { $value = $a.value; }
    | b = literal             { $value = $b.value; }
    | t = ID                  { $value = builder.id($t.text); }
    | t = QuotedID            { $value = builder.id(builder.unquote($t.text)); }
    ;

params returns[List<Expression> value]:
    a = expr                  { $value = new ArrayList<Expression>(); $value.add($a.value); }
    ( ',' b = expr            { $value.add($b.value); } )*
    ;

literal returns[Expression value]:
    t = Number                { $value = builder.number($t.text); }
    | t = QuotedString        { $value = builder.string($t.text); }
    | t = 'true'              { $value = builder.bool(true); }
    | t = 'false'             { $value = builder.bool(false); }
    ;

WS: (' '|'\r'|'\t'|'\u000C'|'\n') -> channel(HIDDEN);

ID: NameStartChar NameChar*;

QuotedID: '`' NameCharacters '`';

QuotedString: '"' StringCharacters '"';

fragment
NameChar
   : NameStartChar
   | '0'..'9'
   | '_'
   | '\u00B7'
   | '\u0300'..'\u036F'
   | '\u203F'..'\u2040'
   ;

fragment
NameStartChar
   : 'A'..'Z' | 'a'..'z'
   | '\u00C0'..'\u00D6'
   | '\u00D8'..'\u00F6'
   | '\u00F8'..'\u02FF'
   | '\u0370'..'\u037D'
   | '\u037F'..'\u1FFF'
   | '\u200C'..'\u200D'
   | '\u2070'..'\u218F'
   | '\u2C00'..'\u2FEF'
   | '\u3001'..'\uD7FF'
   | '\uF900'..'\uFDCF'
   | '\uFDF0'..'\uFFFD'
   ;

fragment
NameCharacters
    : NameCharacter+
    ;

fragment
NameCharacter
    : ~('\\'|'`')
    | EscapeSequence
    ;

fragment
StringCharacters
    : StringCharacter+
    ;

fragment
StringCharacter
    : ~('"'|'\\')
    | UnicodeEscape
    | EscapeSequence
    ;

fragment
EscapeSequence
    : '\\' ('b' | 't' | 'n' | 'f' | 'r' | '\'' | '\\\'')
      | OctalEscape
      | UnicodeEscape
    ;

fragment
OctalEscape
    : '\\' OctalDigit
    | '\\' OctalDigit OctalDigit
    | '\\' ZeroToThree OctalDigit OctalDigit
    ;

fragment
OctalDigit
    : '0'..'7'
    ;

fragment
ZeroToThree
    : '0'..'3'
    ;

fragment
UnicodeEscape
    : '\\' 'u' HexDigit HexDigit HexDigit HexDigit
    ;

fragment
HexDigit
    : ('0'..'9' | 'a'..'f' | 'A'..'F')
    ;

fragment Digits
    : [0-9]+
    ;

Number
    : Digits ('.' Digits)?
    ;