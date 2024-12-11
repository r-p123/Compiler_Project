import java.io.*;
import java.util.HashMap;

public class LexicalAnalyzer{

    private static BufferedReader reader;
    private static HashMap<String, String> reservedWordsMap = new HashMap<>();
    private static int lineNum;
    private static boolean multiLine = false;

    private static String line;
    private static char[] lineChars;
    private static int[] lineCols;
    private static int cursor;

    private static int currChar;

    private void openFileSafely(String s) throws IOException{

        reader = new BufferedReader(new FileReader(s));
        line = reader.readLine();
        lineChars = line.toCharArray();
        lineCols = new int[line.length()];
        lineNum = 1;

        for(int i = 0; i<line.length(); i++){
            lineCols[i] = i+1;
        }

        cursor = 0;

    }

    static{
        String[] keywords = {"void", "int", "double", "bool", "string", "null", "for",
                "while", "if", "else", "return", "break", "Print", "ReadInteger", "ReadLine"};

        String[] values = {"T_Void", "T_Int", "T_Double", "T_BoolConstant", "T_String", "T_Null", "T_For",
                           "T_While", "T_If", "T_Else", "T_Return", "T_Break", "T_Print", "T_ReadInteger", "T_ReadLine"};

        for(int i = 0; i<keywords.length; i++){
            reservedWordsMap.put(keywords[i], values[i]);
        }
    }

    public static void main(String[] args) throws IOException{

        LexicalAnalyzer obj = new LexicalAnalyzer();
        obj.openFileSafely(args[0]);

        Token t = obj.getToken();
        while( !t.attribute.equals("EOF")) {
            System.out.print(t);
            t = obj.getToken();
        }

    }


    public int nextChar() throws IOException{
        if (line==null) {
            return -1;
        }

        if (cursor == lineChars.length){
            cursor++;
            return '\n';
        }else{
            return lineChars[cursor++];
        }

    }

    public Token getToken() throws IOException{
        Token retToken = new Token();
        String state = "start";
        String lexeme = "";
        retToken.setLine(lineNum);

        while(true){
            switch(state){
                case "start":
                    currChar = nextChar();

                    //if we are in a multi-line comment, keep skipping
                    if (multiLine){
                        if( currChar == '*') {
                            currChar = nextChar();
                            if (currChar == '/') {
                                multiLine = false;
                            } else {
                                cursor--;
                                currChar = '*';
                            }


                        }else if(currChar == '\n'){
                            line = reader.readLine();
                            lineNum++;
                            retToken.setLine(lineNum);
                            if (line != null) {
                                while (line != null && line.length() == 0) {

                                    line = reader.readLine();
                                    lineNum++;
                                    retToken.setLine(lineNum);

                                }
                                if (line != null) {
                                    lineChars = line.toCharArray();
                                    lineCols = new int[line.length()];

                                    for (int i = 0; i < line.length(); i++) {
                                        lineCols[i] = i + 1;
                                    }

                                    cursor = 0;
                                } else {
                                    state = "EOF";
                                }

                            } else {
                                state = "EOF";
                            }

                        }
                        continue;

                    }

                    retToken.setStartCol(cursor);
                    if ( isCharacter(currChar)){

                        state = "startsWithLetter";
                        lexeme = lexeme + (char) currChar;
                        retToken.setStartCol(cursor);

                    }else if(isOperator(currChar)){

                        state = "operator";
                        lexeme = lexeme + (char) currChar;
                        retToken.setStartCol(cursor);

                    }else if(isDigit(currChar)) {
                        //can be either  hex or decimal

                        if (currChar == '0') {
                            lexeme = lexeme + (char) currChar;
                            currChar = nextChar();
                            if (currChar == 'x' || currChar == 'X') {

                                currChar = nextChar();
                                if (isDigit(currChar) || isHex(currChar)){
                                    lexeme = lexeme + 'X' +(char) currChar;
                                    state = "startsWithHex";
                                }else{
                                    cursor-=2;
                                    retToken.setEndCol(cursor);
                                    retToken.attribute = String.format("T_IntConstant (value = %s)", Integer.parseInt(lexeme));
                                    retToken.token = lexeme;
                                    return retToken;

                                }

                            }else{
                                state = "startsWithDigit";
                                cursor--;
                            }

                        }else if(isDigit(currChar)){

                            lexeme = lexeme + (char) currChar;
                            state = "startsWithDigit";

                        }


                    }else if (currChar == '"'  ){
                        state = "string";
                        lexeme = lexeme + (char) currChar;

                    }else if(currChar == '\n' || currChar == ' ' || currChar == '\t') {

                        if (currChar == '\n') {
                            line = reader.readLine();
                            lineNum++;
                            retToken.setLine(lineNum);
                            if (line != null) {
                                while (line != null && line.length() == 0) {

                                    line = reader.readLine();
                                    lineNum++;
                                    retToken.setLine(lineNum);

                                }
                                if (line != null) {
                                    lineChars = line.toCharArray();
                                    lineCols = new int[line.length()];

                                    for (int i = 0; i < line.length(); i++) {
                                        lineCols[i] = i + 1;
                                    }

                                    cursor = 0;
                                } else {
                                    state = "EOF";
                                }

                            } else {
                                state = "EOF";
                            }

                        }
                    }else if(currChar == '/'){
                        currChar = nextChar();
                        if (currChar == '/') {
                            cursor = lineChars.length;
                        }else if(currChar == '*'){
                            multiLine = true;
                        }else{
                            cursor--;
                            retToken.attribute = "'/'";
                            retToken.token = "/";
                            retToken.setEndCol(cursor);
                            return retToken;
                        }
                    }else if(currChar == -1){
                        retToken.attribute = "EOF";
                        retToken.token = "EOF";
                        return retToken;

                    }else{
                        //lexeme = lexeme + (char) currChar;
                        retToken.errorMessage = String.format("*** Error line %d.\n*** Unrecognized char: %c", lineNum, (char)currChar);
                        retToken.attribute = "errorMessage";

                        return retToken;
                    }

                    break;

//-------------------------------------------------------------------------------------------------------------
                case "string":

                    currChar = nextChar();

                    while(currChar != '\n' && currChar != '"'){
                        lexeme = lexeme + (char) + currChar;
                        currChar = nextChar();
                    }

                    if (currChar != '"'){
                        cursor--;
                    }

                    retToken.setEndCol(cursor);
                    //have a valid string
                    if(lexeme.length() == 32){
                        retToken.attribute = "Error: String is over 31 chars";
                        retToken.token = lexeme;
                        return retToken;
                    }
                    else if (currChar == '"'){
                        lexeme = lexeme + (char) + currChar;
                        retToken.attribute = String.format("T_StringConstant (value = %s)", lexeme);
                        retToken.token = lexeme;
                        return retToken;
                    }else if(currChar == '\n'){
                        retToken.errorMessage = String.format("\n*** Error line %d.\n*** Unterminated string constant: %s\n", lineNum, lexeme);
                        retToken.attribute = "error";
                        //retToken.token = lexeme;
                        return retToken;
                    }
//-------------------------------------------------------------------------------------------------------------
                case "double":
                    currChar = nextChar();

                    while(  isDigit(currChar) && currChar != -1  ){
                        lexeme = lexeme + (char) currChar;
                        currChar = nextChar();
                    }

                    String temp = "";

                    if(currChar == 'E' || currChar == 'e'){

                        temp = "";  //temp + 'E';
                        currChar = nextChar();

                        if (currChar == '+' || currChar == '-') {
                            int symbol = currChar;
                            currChar = nextChar();

                            int c = 0;
                            while(  isDigit(currChar) && currChar != -1  ){
                                temp = temp + (char) currChar;
                                currChar = nextChar();
                                c++;
                            }
                            if (c==0){
                                cursor-=3;
                                retToken.attribute = String.format("T_DoubleConstant (value = %s)", lexeme);
                                retToken.token = lexeme;
                                retToken.setEndCol(cursor);
                                return retToken;
                            }else{
                                cursor--;

                                Double base = Double.parseDouble(lexeme);

                                if (temp.length() > 0){
                                    int shift = Integer.parseInt(temp);
                                    while(shift>0){
                                        shift--;
                                        if(symbol == '+'){
                                            base = base*10;
                                        }else{
                                            base = base/10;
                                        }

                                    }
                                }

                                retToken.attribute = String.format("T_DoubleConstant (value = %g)", base);
                                retToken.token = lexeme + 'E' +(char)symbol + temp;


                                retToken.setEndCol(cursor);
                                return retToken;
                            }
                        }
                        else if(isDigit(currChar)){
                            temp = temp + '+';
                            temp = temp + (char) currChar;
                            currChar = nextChar();

                            while(  isDigit(currChar) && currChar != -1  ){
                                temp = temp + (char) currChar;
                                currChar = nextChar();
                            }
                            cursor--;

                            Double base = Double.parseDouble(lexeme);

                            if (temp.length() > 0){
                                int shift = Integer.parseInt(temp);
                                while(shift>0){
                                    shift--;
                                    base = base*10;

                                }
                            }

                            retToken.attribute = String.format("T_DoubleConstant (value = %g)", base);
                            retToken.token = lexeme + "E+" + temp;
                            retToken.setEndCol(cursor);
                            return retToken;

                        }else{

                            cursor--;
                            retToken.attribute = String.format("T_DoubleConstant (value = %s)", lexeme);
                            retToken.token = lexeme + temp;
                            retToken.setEndCol(cursor);
                            return retToken;

                        }

                    }else{

                        cursor--;
                        retToken.attribute = String.format("T_DoubleConstant (value = %s)", lexeme);
                        retToken.token = lexeme;
                        retToken.setEndCol(cursor);
                        return retToken;

                    }


//-------------------------------------------------------------------------------------------------------------

                case "startsWithDigit":
                    //decimal or double
                    currChar = nextChar();

                    while(  isDigit(currChar) && currChar != -1  ){
                        lexeme = lexeme + (char) currChar;
                        currChar = nextChar();
                    }

                    if(currChar == '.'){
                        lexeme = lexeme + (char) currChar;
                        state = "double";
                        break;
                    }

                    cursor--;
                    retToken.attribute = String.format("T_IntConstant (value = %s)", Integer.parseInt(lexeme));
                    retToken.token = lexeme;
                    retToken.setEndCol(cursor);
                    return retToken;

//-------------------------------------------------------------------------------------------------------------

                case "startsWithHex":

                    currChar = nextChar();

                    while(  isHex(currChar) && currChar != -1  ){
                        lexeme = lexeme + (char) currChar;
                        currChar = nextChar();
                    }

                    cursor--;
                    retToken.attribute = "integer";
                    retToken.token = lexeme;
                    retToken.setEndCol(cursor);
                    return retToken;
//-------------------------------------------------------------------------------------------------------------
                case "startsWithLetter":

                    currChar = nextChar();

                    while((isCharacter(currChar) || isUnderScore(currChar) || isDigit(currChar) )&& currChar != -1){
                        lexeme = lexeme + (char) currChar;
                        currChar = nextChar();
                    }

                    cursor--;
                    retToken.setEndCol(cursor);
                    retToken.token = lexeme;

                    if (lexeme.length() >= 31){

                        retToken.errorMessage = String.format("*** Error line %d.\n*** Identifier too long: %s\n\n", lineNum, lexeme);


                        retToken.attribute = String.format("T_Identifier (truncated: %s)", lexeme.substring(0, 31));

                    }else if (lexeme.equals("true")){

                        retToken.attribute = "T_BoolConstant (value = true)";

                    }else if (lexeme.equals("false")){

                        retToken.attribute = "T_BoolConstant (value = false)";

                    }else if (reservedWordsMap.containsKey(lexeme)){

                        retToken.attribute = reservedWordsMap.get(lexeme);

                    }else{

                        retToken.attribute = "T_Identifier";

                    }
                    return retToken;

//-------------------------------------------------------------------------------------------------------------
                case "operator":
                    /* operators that could be something else:
                        < <=
                        > >=
                        = ==
                        ! !=
                    */
                    if( currChar == '+' || currChar == '-' || currChar == '*' ||
                        currChar == '%' || currChar == ';' || currChar == ',' || currChar == '.' ||
                        currChar == '(' || currChar == ')' || currChar == '{' || currChar == '}' ||
                        currChar == '[' || currChar == ']'  )
                    {

                        retToken.attribute = String.format("'%c'", (char)currChar);
                        retToken.token = lexeme;
                        retToken.setEndCol(cursor);

                    }else if(currChar == '&'){
                        currChar = nextChar();
                        if (currChar == '&'){
                            lexeme = lexeme + (char) currChar;
                            retToken.attribute = "T_And";
                            retToken.token = lexeme;
                            retToken.setEndCol(cursor);
                            return retToken;
                        }else{
                            cursor--;
                            retToken.errorMessage = String.format("*** Error line %d.\n*** Unrecognized char: &", lineNum);
                            retToken.attribute = "error";
                            return retToken;
                        }
                    }else if(currChar == '|'){
                        currChar = nextChar();
                        if (currChar == '|'){
                            lexeme = lexeme + (char) currChar;
                            retToken.attribute = "T_Or";
                            retToken.token = lexeme;
                            retToken.setEndCol(cursor);
                            return retToken;
                        }else{
                            cursor--;
                            retToken.errorMessage = String.format("*** Error line %d.\n*** Unrecognized char: |", lineNum);
                            retToken.attribute = "error";
                            return retToken;
                        }
                    }else if(currChar == '!'){

                        currChar = nextChar();

                        if (currChar == '='){
                            lexeme = lexeme + (char) currChar;
                            retToken.token = lexeme;
                            retToken.attribute = "T_NotEqual";
                            retToken.setEndCol(cursor);

                        }else{
                            cursor--;
                            retToken.token = lexeme;
                            retToken.attribute = "'!'";
                            retToken.setEndCol(cursor);
                        }

                        return retToken;

                    }else if(currChar == '='){

                        currChar = nextChar();

                        if (currChar == '='){
                            lexeme = lexeme + (char) currChar;
                            retToken.token = lexeme;
                            retToken.attribute = "T_Equal";
                            retToken.setEndCol(cursor);

                        }else{
                            cursor--;
                            retToken.token = lexeme;
                            retToken.attribute = "'='";
                            retToken.setEndCol(cursor);
                        }

                        return retToken;

                    }else if(currChar == '<'){

                        currChar = nextChar();

                        if (currChar == '='){
                            lexeme = lexeme + (char) currChar;
                            retToken.token = lexeme;
                            retToken.attribute = "T_LessEqual";
                            retToken.setEndCol(cursor);

                        }else{
                            cursor--;
                            retToken.token = lexeme;
                            retToken.attribute = "'<'";
                            retToken.setEndCol(cursor);
                        }

                        return retToken;

                    }else if (currChar == '>'){

                        currChar = nextChar();

                        if (currChar == '='){
                            lexeme = lexeme + (char) currChar;
                            retToken.token = lexeme;
                            retToken.attribute = "T_GreaterEqual";
                            retToken.setEndCol(cursor);

                        }else{
                            cursor--;
                            retToken.token = lexeme;
                            retToken.attribute = "'>'";
                            retToken.setEndCol(cursor);
                        }

                        return retToken;

                    }

                    return retToken;

                case "EOF":
                    retToken.attribute = "EOF";
                    retToken.token = "EOF";
                    retToken.setEndCol(cursor);
                    return retToken;

                default:
                    System.out.println("\n*****\n!!!!! Something very wrong !!!!!\n*****\n");
                    return retToken;
            }

        }

    }

    private static boolean isOperator(int currChar){
        char[] ops = {  '+', '-', '*', '%', '<', '>', '=', '!',
                '&', '|', ';', ',', '.', '(', ')', '{', '}'  };

        for(char s : ops){
            if (currChar == s){
                return true;
            }
        }
        return false;
    }

    private static boolean isCharacter(int c){
        if ( (c >=65 & c <= 90) || (c >= 97 && c <= 122) ){
            return true;
        }
        return false;
    }

    private static boolean isUnderScore(int c){
        if ( c== '_' ){
            return true;
        }
        return false;
    }

    private static boolean isDigit(int c){
        if ( c >= 48 && c <= 57){
            return true;
        }
        return false;
    }

    private static boolean isHex(int c){
        if (  isDigit(c) || ( c>=97 && c<=102 ) || ( c>=65 && c<=70 ) ){
            return true;
        }
        return false;
    }

}
