import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class Parser {

    static public List<String> fileLines;
    private final List<Token> tokens;
    private int current = 0;


    static public ArrayList<StringBuilder> semanticErrors = new ArrayList<>();
    static public ArrayList<Integer> semanticErrLineNums = new ArrayList<>();


    Parser(List<Token> tokens){
        this.tokens = tokens;
    }
    public void printTokens(){
        for(Token t : tokens){
            System.out.println(t);
        }
    }


    public static void main(String[] args) throws IOException {

        String fileName = args[0];  //"t4.decaf";
        fileLines =  Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);

        List<Token> tokens = new ArrayList<>();

        LexicalAnalyzer obj = new LexicalAnalyzer();

        obj.openFileSafely(fileName);

        Token t = obj.getToken();
        while( !t.attribute.equals("EOF")) {
            tokens.add(t);
            t = obj.getToken();
        }

        tokens.add(t);
        Parser parser = new Parser(tokens);

        //parser.printTokens();

        ASTNode root = parser.parse();

        if(root instanceof ErrorNode){
            System.out.println(((ErrorNode) root).errorStr);
        }else{
            //String ans = new AstPrinter().print(root);
            //System.out.println(ans);

            String s1 = new CheckOperands().traverse(root);
            ExprNode.flag = false;
            String s2 = new CheckBreakStmts().traverse(root);
            String s3 = new CheckReturn().traverse(root);
            String s4 = new CheckFuncArgs().traverse(root);



            Parser.insertionSort(Parser.semanticErrLineNums, Parser.semanticErrors);

            if(Parser.semanticErrLineNums.size() > 0) {
                for (StringBuilder str : Parser.semanticErrors) {

                    System.out.println(str);

                }
                //System.out.println(Parser.semanticErrLineNums);
            }else {

                String s5 = new CheckMainFunction().traverse(root);
                if (s5 == null) {
                    String s6 = new CodeGen().traverse(root);

                    System.out.println("\n#########\n");
                    List<String> asmFile =  Files.readAllLines(Paths.get("defs.asm"), StandardCharsets.UTF_8);
                    for(String str : asmFile){
                        System.out.println(str);
                    }


                } else {
                    System.out.println(s5);
                }
            }

        }

    }

    static void insertionSort(ArrayList<Integer> A, ArrayList<StringBuilder> B) {

        int n = A.size();

        for (int i = 1; i < n; i++) {
            Integer keyA = A.get(i);
            StringBuilder keyB = B.get(i);

            int j = i - 1;

            while (j >= 0 && keyA < A.get(j)) {
                A.set(j+1, A.get(j));
                B.set(j+1, B.get(j));
                --j;
            }
            A.set(j+1, keyA);
            B.set(j+1, keyB);
        }

    }

    public ASTNode parse() {

        try{
            return Program();
        } catch (SyntaxErrorException e){

            int lineNum = tokens.get(current).getLineNum();

            StringBuilder error = new StringBuilder();
            error.append("*** Error line " + lineNum + "\n");
            String lineN = fileLines.get(lineNum-1);
            error.append(lineN + "\n");

            int colNum = tokens.get(current).getStartCol();
            int tokenLength = tokens.get(current).token.length();

            for(int i = 1; i < colNum; i++){
                error.append(" ");
            }

            for(int i = 1; i <= tokenLength; i++){
                error.append("^");
            }

            error.append("\n*** syntax error");

            return new ErrorNode(error.toString());
        } catch(EOFException e){
            return new ErrorNode("End of file reached\n");
        }

    }

    private ASTNode Program() throws SyntaxErrorException, EOFException{

        ProgramNode programNode = new ProgramNode();
        if(tokens.get(current).attribute.equals("EOF")){
            throw new EOFException();
        }

        while(!tokens.get(current).attribute.equals("EOF")){
            programNode.decls.add(Decl());
        }

        return programNode;

    }

    private ASTNode Decl() throws SyntaxErrorException{

        if (compareVals(current, "T_Int", "T_Double", "T_BoolConstant", "T_String", "T_Void")){

            if(tokens.get(current+1).attribute.equals("T_Identifier")){

                if(tokens.get(current+2).token.equals("(")){
                    return functionDecl();
                }else{
                    return variableDecl();
                }

            }else{
                throw new SyntaxErrorException();
            }

        }else{
            throw new SyntaxErrorException();
        }

    }

    private VariableNode variable() throws SyntaxErrorException {

        String type = tokens.get(current++).token;
        if(tokens.get(current).attribute.split(" ")[0].equals("T_Identifier")){

            Ident var = new Ident(tokens.get(current).token, null, tokens.get(current).getLineNum(), tokens.get(current).getStartCol());
            current++;
            return new VariableNode(type, var, tokens.get(current).getLineNum());
        }else{
            throw new SyntaxErrorException();
        }



    }


    private ASTNode variableDecl() throws SyntaxErrorException{
        if (compareVals(current, "T_Int", "T_Double", "T_BoolConstant", "T_String")){


            VariableNode variableNode = variable();

            if(tokens.get(current).token.equals(";")){
                current++;
                return variableNode;
            }else{
                throw new SyntaxErrorException();
            }

        }

        throw new SyntaxErrorException();

    }

    private FunctionNode functionDecl() throws SyntaxErrorException{

        String returnType = tokens.get(current++).token;
        String functionIdent = tokens.get(current++).token;
        current++; //for the opening parenthesis
        ArrayList<VariableNode> formalNodes = formals();

        StmtBlockNode stmtBlock = statementBlock();
        if(current<tokens.size()-1 && tokens.get(current+1).token.equals("EOF")){
            current++;
        }
        FunctionNode functionNode = new FunctionNode(returnType,  functionIdent, formalNodes, stmtBlock);
        functionNode.numArgs = formalNodes.size();

        return functionNode;
    }

    private StmtBlockNode statementBlock() throws SyntaxErrorException{
        StmtBlockNode stmtblockNode = new StmtBlockNode();
        if(tokens.get(current).token.equals("{")){
            current++;

            while(compareVals(current, "T_Int", "T_Double", "T_BoolConstant", "T_String")){
                stmtblockNode.varDecls.add(variableDecl());
            }

            boolean flag = false;
            while(!tokens.get(current).token.equals("}")){
                flag = true;
                stmtblockNode.stmts.add(stmt());
            }

            if(tokens.get(current).token.equals("}") && !tokens.get(current+1).token.equals("EOF")){
                current++;
            }
            if(flag && tokens.get(current+1).token.equals("EOF")){
                current++;
            }
        }

        return stmtblockNode;

    }

    private ASTNode stmt() throws SyntaxErrorException{

        String s = tokens.get(current).attribute.split("\\(")[0].trim();
        if(s.equals("T_If") && tokens.get(current+1).token.equals("(")){

            current+=2;
            return IfStmtBlock();

        }else if(s.equals("T_While") && tokens.get(current+1).token.equals("(") ){

            current+=2;
            return WhileStmtBlock();

        }else if(s.equals("T_For") && tokens.get(current+1).token.equals("(") ){

            current+=2;
            return ForStmtBlock();

        }else if(s.equals("T_Break")){

            current++;
            if(tokens.get(current).attribute.equals(";")){
                current++;
                return new BreakStmtNode(tokens.get(current-2).getLineNum(), tokens.get(current-2).getStartCol());
            }else{
                throw new SyntaxErrorException();
            }

        }else if(s.equals("T_Return")){

            current++;
            if(tokens.get(current).attribute.equals(";")){

                ReturnStmtNode retStmtNode = new ReturnStmtNode(tokens.get(current).getLineNum(), tokens.get(current).getStartCol());
                current++;
                return retStmtNode;

            }else if(compareVals(tokens.get(current).attribute.split(" ")[0].trim(),
                    "T_Identifier", "T_IntConstant", "T_DoubleConstant", "T_BoolConstant","T_StringConstant", "T_ReadInteger", "T_ReadLine","T_Null", "(")){

                ExprNode exprNode = expr();

                if(tokens.get(current).attribute.equals(";")) {
                    current++;
                }else{
                    throw new SyntaxErrorException();
                }

                return new ReturnStmtNode(exprNode, tokens.get(current-1).getLineNum(), tokens.get(current-2).getStartCol());

            }else{

                throw new SyntaxErrorException();

            }

        }else if(s.equals("T_Print") && tokens.get(current+1).token.equals("(") ){
            current+=2;
            PrintStmtNode printStmtNode = printStmt();
            return printStmtNode;
        }else if(compareVals(s,
                "T_Identifier", "T_IntConstant", "T_DoubleConstant", "T_BoolConstant","T_StringConstant", "T_ReadInteger", "T_ReadLine","T_Null")){

            ListOfExpr listOfExpr = new ListOfExpr();
            while(compareVals(tokens.get(current).attribute.split("\\(")[0].trim(),
                    "T_Identifier", "T_IntConstant", "T_DoubleConstant", "T_BoolConstant","T_StringConstant", "T_ReadInteger", "T_ReadLine","T_Null")){

                listOfExpr.exprList.add(expr());
                if(!tokens.get(current).token.equals(";")){
                    throw new SyntaxErrorException();
                }
                current++;

            }

            return listOfExpr;

        }else if(s.equals("{")){

            StmtBlockNode stmtBlockNode  = statementBlock();

            return stmtBlockNode;
        }

        throw new SyntaxErrorException();
    }

    private ForStmtNode ForStmtBlock() throws SyntaxErrorException{
        ExprNode expr1 = null;
        ExprNode expr2 = null;
        ExprNode expr3 = null;

        ASTNode stmtNode;

        if(tokens.get(current).token.equals(";")){
            current++;
            expr1 = null;
        }else{
            expr1 = expr();
        }
        if(tokens.get(current).token.equals(";")){
            current++;
//            throw new SyntaxErrorException();
        }

        int exprStartIdx =  tokens.get(current).getStartCol();
        expr2 = expr();
        int exprEndIdx = tokens.get(current).getStartCol();


        if(tokens.get(current).token.equals(";")){
            current++;

            if(tokens.get(current).token.equals(")")) {
                current++;
                expr3 = null;
            }else{
                expr3 = expr();
                if(tokens.get(current).token.equals(")")) {
                    current++;
                }else{
                    throw new SyntaxErrorException();
                }
            }

        }

        stmtNode = stmt();

        return new ForStmtNode(expr1, expr2, expr3, stmtNode, exprStartIdx, exprEndIdx);
    }

    private PrintStmtNode printStmt() throws SyntaxErrorException{
        PrintStmtNode printStmtNode = new PrintStmtNode();

        if(tokens.get(current).token.equals(")")){
            throw new SyntaxErrorException();
        }
        while(!tokens.get(current).token.equals(")")){
            printStmtNode.exprNodes.add(expr());
            if(tokens.get(current).token.equals(",")){
                current++;
            }
        }
        current++;

        if(tokens.get(current).token.equals(";")){
            current++;
        }else{
            throw new SyntaxErrorException();
        }
        return printStmtNode;

    }

    private WhileStmtNode WhileStmtBlock() throws SyntaxErrorException{
        ExprNode expr;

        expr = expr();
        if(tokens.get(current).token.equals(")")){
            current++;
        }else{
            throw new SyntaxErrorException();
        }
        ASTNode stmt = stmt();

        return new WhileStmtNode(expr, stmt);
    }

    private IfStmtNode IfStmtBlock() throws SyntaxErrorException{

        ExprNode expr; ASTNode stmt1 = null; ASTNode stmt2 = null;

        int exprStartIdx = tokens.get(current).getStartCol();
        expr = expr();
        int exprEndIdx = tokens.get(current).getStartCol();

        if(tokens.get(current).token.equals(")")){
            current++;
            stmt1 = stmt();
        }else{
            throw new SyntaxErrorException();
        }
        if(tokens.get(current).attribute.equals("T_Else")){
            current++;
            stmt2 = stmt();
        }
        return new IfStmtNode(expr, stmt1, stmt2, exprStartIdx, exprEndIdx);
    }

    private ExprNode expr() throws SyntaxErrorException{

        if(tokens.get(current).attribute.equals("T_Identifier") && tokens.get(current+1).attribute.equals("=")){

            int operatorColStart = tokens.get(current+1).getStartCol();

            Ident ident = new Ident(tokens.get(current).token, null, tokens.get(current).getLineNum(),tokens.get(current).getStartCol());
            String operator = "=";

            current+=2;
            ExprNode expr = expr();

            expr = new ExprNode(ident, operator, expr, tokens.get(current).getLineNum(), operatorColStart);

            return expr;


        }else{
            ExprNode exprNode = andExpr();

            while(tokens.get(current).attribute.equals("T_Or")){
                String operator = "||";
                int operatorColStart = tokens.get(current).getStartCol();
                current++;
                ExprNode right = andExpr();
                exprNode = new ExprNode(exprNode, operator, right, tokens.get(current).getLineNum(), operatorColStart);
            }

            return exprNode;
        }


    }

    private ExprNode andExpr() throws SyntaxErrorException{

        ExprNode exprNode = eqExpr();

        while(tokens.get(current).attribute.equals("T_And")){
            int operatorColStart = tokens.get(current).getStartCol();
            String operator = "&&";
            current++;
            ExprNode right = eqExpr();
            exprNode = new ExprNode(exprNode, operator, right, tokens.get(current).getLineNum(), operatorColStart);
        }

        return exprNode;
    }

    private ExprNode eqExpr() throws SyntaxErrorException{

        ExprNode left = relExpr();

        String operator = "";
        if (tokens.get(current).attribute.equals("T_NotEqual")){
            current++;
            operator = "!=";
        }
        if (tokens.get(current).attribute.equals("T_Equal")){
            current++;
            operator = "==";
        }
        if(operator.length() > 0){
            int operatorColStart = tokens.get(current).getStartCol();
            ExprNode right = relExpr();
            left = new ExprNode(left, operator, right, tokens.get(current).getLineNum(), operatorColStart);
        }

        return left;

    }

    private  ExprNode relExpr() throws SyntaxErrorException{

        ExprNode expr = arithExpr();

        String operator = "";

        if(tokens.get(current).attribute.equals("T_Greater")){
            current++;
            operator = ">";
        } else if(tokens.get(current).attribute.equals("T_GreaterEqual")){
            current++;
            operator = ">=";
        }else if(tokens.get(current).attribute.equals("T_Less")){
            current++;
            operator = "<";
        }else if(tokens.get(current).attribute.equals("T_LessEqual")){
            current++;
            operator = "<=";
        }


        if(operator.length() > 0){
            int operatorColStart = tokens.get(current).getStartCol();
            ExprNode right = arithExpr();

            expr = new ExprNode(expr, operator, right, tokens.get(current).getLineNum(), operatorColStart);
        }

        return expr;
    }

    private ExprNode arithExpr() throws SyntaxErrorException{

        ExprNode expr = term();
        while (tokens.get(current).token.equals("+") || tokens.get(current).token.equals("-")) {
            String op = tokens.get(current).token;
            int operatorColStart = tokens.get(current).getStartCol();
            current++;
            ExprNode right = term();
            expr = new ExprNode(expr, op, right, tokens.get(current).getLineNum(), operatorColStart);
        }
        return expr;
    }

    private ExprNode term() throws SyntaxErrorException{

        ExprNode expr = factor();
        while (tokens.get(current).token.equals("*") || tokens.get(current).token.equals("/") || tokens.get(current).token.equals("%")) {
            String op = tokens.get(current).token;

            int operatorColStart = tokens.get(current).getStartCol();
            current++;
            ExprNode right = factor();

            expr = new ExprNode(expr, op, right, tokens.get(current).getLineNum(), operatorColStart);
        }
        return expr;

    }

    private ExprNode factor() throws SyntaxErrorException{

        String operator = "";

        while (tokens.get(current).token.equals("!") || tokens.get(current).token.equals("-")) {
            operator = operator + tokens.get(current).token;
            current++;
        }

        if(operator.length() == 0){
            operator = null;
        }
        String s = tokens.get(current).attribute.split(" ")[0].trim();
        int operatorColStart = tokens.get(current).getStartCol();

//        "T_Identifier", "T_IntConstant", "T_DoubleConstant", "T_BoolConstant","T_StringConstant", "T_ReadInteger", "T_ReadLine","T_Null"
        if (tokens.get(current).attribute.equals("T_Identifier")) {

            if (tokens.get(current + 1).token.equals("(")) {

                Ident ident = new Ident(tokens.get(current).token, null, tokens.get(current).getLineNum(), tokens.get(current).getStartCol());
                current+=2;

                ActualNode actualNode = new ActualNode();
                actualNode.exprList = actuals();

                if (tokens.get(current).token.equals(")")) {
                    current++;
                    return new CallNode(ident, actualNode, tokens.get(current).getLineNum());
                } else {
                    throw new SyntaxErrorException();
                }

            } else {
                Ident ident = new Ident(tokens.get(current).token, operator, tokens.get(current).getLineNum(), tokens.get(current).getStartCol());
                ExprNode exprNode = new ExprNode(ident, operator, null, tokens.get(current).getLineNum(), operatorColStart);

                current++;
                return exprNode;
            }

        } else if (s.equals("T_IntConstant")) {
            IntNode intNode = new IntNode(tokens.get(current).token, operator, tokens.get(current).getLineNum());
            ExprNode exprNode = new ExprNode(intNode, operator, null, tokens.get(current).getLineNum(), operatorColStart);
            current++;

            return exprNode;

        } else if (s.equals("T_DoubleConstant")) {

            DoubleNode doubleNode = new DoubleNode(tokens.get(current).token, operator, tokens.get(current).getLineNum());
            ExprNode exprNode = new ExprNode(doubleNode, operator, null, tokens.get(current).getLineNum(), operatorColStart);
            current++;
            return exprNode;

        } else if (s.equals("T_BoolConstant")) {

            BoolNode boolNode = new BoolNode(tokens.get(current).token, operator, tokens.get(current).getLineNum());
            ExprNode exprNode = new ExprNode(boolNode, operator, null, tokens.get(current).getLineNum(), operatorColStart);
            current++;
            return exprNode;

        } else if (s.equals("T_StringConstant")) {

            StringNode stringNode = new StringNode(tokens.get(current).token, operator, tokens.get(current).getLineNum());
            ExprNode exprNode = new ExprNode(stringNode, operator, null, tokens.get(current).getLineNum(), operatorColStart);
            current++;
            return exprNode;

        } else if (s.equals("T_ReadInteger")) {

            ReadIntNode readIntNode = new ReadIntNode(tokens.get(current).token, operator, tokens.get(current).getLineNum());
            ExprNode exprNode = new ExprNode(readIntNode, operator, null, tokens.get(current).getLineNum(), operatorColStart);

            if(tokens.get(current+1).token.equals("(") && tokens.get(current+2).token.equals(")") ){
                current+=2;
            }else{
                throw new SyntaxErrorException();
            }
            current++;
            return exprNode;

        } else if (s.equals("T_ReadLine")) {

            ReadLineNode readLineNode = new ReadLineNode(tokens.get(current).token, operator, tokens.get(current).getLineNum());
            ExprNode exprNode = new ExprNode(readLineNode, operator, null, tokens.get(current).getLineNum(), operatorColStart);

            if(tokens.get(current+1).token.equals("(") && tokens.get(current+2).token.equals(")") ){
                current+=2;
            }else{
                throw new SyntaxErrorException();
            }
            current++;

            return exprNode;

        } else if (s.equals("T_Null")) {
            NullNode nullNode = new NullNode(tokens.get(current).token, operator, tokens.get(current).getLineNum());
            ExprNode exprNode = new ExprNode(nullNode, operator, null, tokens.get(current).getLineNum(), operatorColStart);
            current++;

            return exprNode;

        } else if (s.equals("(")){
            current++;
            ExprNode expr = expr();
            if(tokens.get(current).attribute.equals(")")){
                current++;
                return expr;
            }else{
                throw new SyntaxErrorException();
            }

        }


        throw new SyntaxErrorException();
    }

    private ArrayList<ExprNode> actuals() throws SyntaxErrorException{

        ArrayList<ExprNode> exprList = new ArrayList<>();

        if (tokens.get(current).token.equals( ")" )){
            return exprList;
        }
        exprList.add(expr());

        while(tokens.get(current).token.equals( "," )) {
            current++;
            exprList.add(expr());
        }

        return exprList;
    }


    private ArrayList<VariableNode> formals()  throws SyntaxErrorException{

        ArrayList<VariableNode> formalNodes = new ArrayList<VariableNode>();
        if (tokens.get(current).token.equals( ")" )){
            current++;
            return formalNodes;
        }
        formalNodes.add(variable());
        while(tokens.get(current).token.equals( "," )) {
            current++;
            formalNodes.add(variable());
        }

        if(tokens.get(current).token.equals( ")" )){
            current++;
        }else{
            throw new SyntaxErrorException();
        }

        return formalNodes;

    }


    private boolean compareVals(int curr, String... types) throws SyntaxErrorException{
        for (String type : types) {
            if (tokens.get(curr).attribute.equals(type)) {
                return true;
            }
        }
        return false;
    }


    private boolean compareVals(String curr, String... types) throws SyntaxErrorException{
        for (String type : types) {
            if (curr.equals(type)) {
                return true;
            }
        }
        return false;
    }


}
