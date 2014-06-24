package de.codesourcery.tinyscript.parser;

public enum TokenType 
{
	OPERATOR,
	IDENTIFIER,
	NUMBER,
	COMMA,
	DOT,
	PARENS_OPEN,
	PARENS_CLOSE,
	STRING_DELIMITER,
	ESCAPE_CHARACTER,
	SEMICOLON,
	TEXT,
	TRUE,
	FALSE;
}